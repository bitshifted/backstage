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
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.util.logger
import co.bitshifted.ignite.common.model.OperatingSystem
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString

class MacDeploymentBuilder(val builder: DeploymentBuilder){

    private val APP_BUNDLE_JRE_DIR = "Contents/MacOS/jre"
    private val appBundleMacOSDirPath = "Contents/MacOS"
    private val appBundleResourcesDirPath = "Contents/Resources"
    private val infoPlistTemplate = "info.plist.ftl"

    val logger = logger(this)
    lateinit var classpathDir: Path
    lateinit var modulesDir: Path
    lateinit var resourcesDIr : Path
    lateinit var macOsDir : Path

    fun build() : Boolean {
        logger.info("Creating Mac OS X deployment in directory {}", builder.macDir)
        try {
            createDirectoryStructure()
            builder.copyDependencies(modulesDir, classpathDir, OperatingSystem.MAC)
            builder.copyResources(resourcesDIr)
            builder.buildJdkImage(macOsDir, modulesDir, OperatingSystem.MAC)
            copyLauncher()
            copyMacIcons()
            copySplashScreen()
            createInfoPlist()
            logger.info("Successfully created Mac OS X deployment in directory {}", builder.macDir)
            return true
        } catch(th : Throwable) {
            logger.error("Error building Mac OS X deployment", th)
            throw  th
        }
    }

    private fun createDirectoryStructure() {
        macOsDir = Files.createDirectories(builder.macDir.resolve(appBundleMacOSDirPath))
        classpathDir = Files.createDirectories(builder.macDir.resolve(appBundleMacOSDirPath).resolve(BackstageConstants.OUTPUT_CLASSPATH_DIR))
        logger.info("Created classpath directory at {}", classpathDir.toFile().absolutePath)
        modulesDir = Files.createDirectories(builder.macDir.resolve(appBundleMacOSDirPath).resolve(BackstageConstants.OUTPUT_MODULES_DIR))
        logger.info("Created modules directory at {}", modulesDir.toFile().absolutePath)
        resourcesDIr = Files.createDirectories(builder.macDir.resolve(appBundleResourcesDirPath))
        logger.info("Created Resources directory in {}", resourcesDIr.absolutePathString())
    }

    private fun copyLauncher() {
        val launcherPath = Path.of(builder.launchCodeDir.absolutePathString(),
            BackstageConstants.OUTPUT_LAUNCHER_DIST_DIR,
            BackstageConstants.LAUNCHER_NAME_MAC
        )
        logger.debug("Copying Mac OS X launcher from {} to {}", launcherPath.absolutePathString(), macOsDir.resolve(BackstageConstants.LAUNCHER_NAME_MAC))
        Files.copy(launcherPath, macOsDir.resolve(builder.builderConfig.deploymentConfig.applicationInfo.exeName), StandardCopyOption.COPY_ATTRIBUTES)
    }

    private fun copyMacIcons() {
        builder.builderConfig.deploymentConfig.applicationInfo.mac.icons.forEach {
            val name = if (it.target != null) it.target else it.source
            val target = resourcesDIr.resolve(name)
            logger.debug("Icon target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.builderConfig.contentService?.get(it.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun copySplashScreen() {
        val splash = builder.builderConfig.deploymentConfig.applicationInfo.splashScreen
        if (splash!= null) {
            val name = if (splash.target != null) splash.target else splash.source
            val target = macOsDir.resolve(name)
            logger.debug("Splash screen target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.builderConfig.contentService?.get(splash.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun createInfoPlist() {
        val data = mutableMapOf<String, String>()
        data["appName"] = builder.builderConfig.deploymentConfig.applicationInfo.name
        data["appExecutable"] = builder.builderConfig.deploymentConfig.applicationInfo.exeName
        data["appIcon"] = builder.builderConfig.deploymentConfig.applicationInfo.mac.icons[0].target
        data["appId"] = builder.builderConfig.deploymentConfig.applicationId
        data["bundleFqdn"] = builder.builderConfig.deploymentConfig.applicationId
        data["appVersion"] = builder.builderConfig.deploymentConfig.version

        val template = builder.freemarkerConfig.getTemplate(infoPlistTemplate)
        val targetPath = macOsDir.parent.resolve("Info.plist")
        val writer = FileWriter(targetPath.toFile())
        writer.use {
            template.process(data, writer)
        }
    }
}