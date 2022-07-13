/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service.deployment.builders

import co.bitshifted.backstage.BackstageConstants
import co.bitshifted.backstage.BackstageConstants.JAR_EXTENSION
import co.bitshifted.backstage.BackstageConstants.OUTPUT_CLASSPATH_DIR
import co.bitshifted.backstage.BackstageConstants.OUTPUT_MODULES_DIR
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.DeploymentException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.service.ResourceMapping
import co.bitshifted.backstage.util.logger
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


open class DeploymentBuilder(val config : DeploymentBuilderConfig) {

    val logger = logger(this)
    lateinit var classpathDir : Path
    lateinit var modulesDir : Path

    @Autowired lateinit  var resourceMapping : ResourceMapping

    fun build() : Boolean {
        try {
            createDirectoryStructure()
            copyDependencies()
            copyResources()
            buildJdkImage()
        } catch(ex : Throwable) {
            logger.error("Failed to build deployment", ex)
            return false
        }
        return true
    }

    protected fun createDirectoryStructure() {
        classpathDir = Files.createDirectories(Paths.get(config.baseDir.toFile().absolutePath, OUTPUT_CLASSPATH_DIR))
        logger.info("Created classpath directory at {}", classpathDir.toFile().absolutePath)
        modulesDir = Files.createDirectories(Paths.get(config.baseDir.toFile().absolutePath, OUTPUT_MODULES_DIR))
        logger.info("Created modules directory at {}", modulesDir.toFile().absolutePath)
    }

    protected fun copyDependencies() {
        config.deployment.jvmConfiguration?.dependencies?.forEach {
            var targetDIr : Path
            if (it.isModular) {
                targetDIr = modulesDir
                logger.debug("Copying dependency {}:{}:{} to {}", it.groupId, it.artifactId, it.version, targetDIr)
            } else {
                targetDIr = classpathDir
            }
            val targetPath = Paths.get(targetDIr.toFile().absolutePath, it.sha256 + JAR_EXTENSION)
            config.contentService?.get(it.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    protected fun copyResources() {
        config.deployment.resources.forEach {
            val target = config.baseDir.resolve(it.target)
            logger.debug("Resource target: {}", target.toFile().absolutePath)
            // create directory structure
            Files.createDirectories(target.parent)
            config.contentService?.get(it.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }

        }
    }

    protected fun buildJdkImage() {
        logger.info("Building JDK image")
        val jvmConfig = config.deployment.jvmConfiguration ?: throw DeploymentException("Can not find JVm configuration")
        val jdkLocation = resourceMapping.getJdkLocation(jvmConfig.vendor, jvmConfig.majorVersion, jvmConfig.fixedVersion ?: "")
        val moduleDirs = listOf(Path.of(jdkLocation).resolve(BackstageConstants.JDK_JMODS_DIR_NAME), modulesDir)
        val jreOutputDir = config.baseDir.resolve(BackstageConstants.OUTPUT_JRE_DIR)
        val toolRunner = ToolsRunner(config.baseDir)
        val jdkModules = toolRunner.getJdkModules()
        logger.debug("JDK modules to include: {}", jdkModules)
        if (jdkModules.isEmpty()) {
            throw DeploymentException("Failed to get any JDK module")
        }
        toolRunner.createRuntimeImage(jdkModules, moduleDirs, jreOutputDir)
    }

}