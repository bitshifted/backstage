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
import co.bitshifted.backstage.exception.DeploymentException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.util.logger
import co.bitshifted.backstage.util.safeAppName
import co.bitshifted.ignite.common.model.OperatingSystem
import org.apache.commons.io.FileUtils
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isSymbolicLink

class MacDeploymentBuilder(val builder: DeploymentBuilder){

    private val appBundleMacOSDirPath = "Contents/MacOS"
    private val appBundleResourcesDirPath = "Contents/Resources"
    private val infoPlistTemplate = "mac/info.plist.ftl"
    private val infoPlistFile = "Info.plist"
    private val createDmgTemplate = "mac/create-dmg.sh.ftl"
    private val createDmgScriptFileName = "create-dmg.sh"
    private val mbBytes = 1_000_000
    private val dsStoreInput = "/templates/mac/DS_Store"
    private val dsStoreFile = "DS_Store"
    private val backgroundImgInput = "/templates/mac/appforge-background.png"
    private val backgroundImageFile = "appforge-background.png"

    val logger = logger(this)
    lateinit var classpathDir: Path
    lateinit var modulesDir: Path
    lateinit var resourcesDir : Path
    lateinit var macOsDir : Path

    fun build() : Boolean {
        logger.info("Creating Mac OS X deployment in directory {}", builder.macDir)
        try {
            createDirectoryStructure()
            builder.copyDependencies(modulesDir, classpathDir, OperatingSystem.MAC)
            builder.copyResources(macOsDir)
            builder.buildJdkImage(macOsDir, modulesDir, OperatingSystem.MAC)
            copyLauncher()
            copyMacIcons()
            copySplashScreen()
            createInfoPlist()
            moveNonExecutableFiles()
            createInstaller()
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
        resourcesDir = Files.createDirectories(builder.macDir.resolve(appBundleResourcesDirPath))
        logger.info("Created Resources directory in {}", resourcesDir.absolutePathString())
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
            val target = resourcesDir.resolve(name)
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
            val target = resourcesDir.resolve(name)
            logger.debug("Splash screen target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.builderConfig.contentService?.get(splash.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
            // create symlink to splash screen
            Files.createSymbolicLink(macOsDir.resolve(name), macOsDir.relativize(resourcesDir.resolve(target.fileName)))
        }
    }

    private fun getTemplateData() : MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>()
        data["appName"] = builder.builderConfig.deploymentConfig.applicationInfo.name
        data["appExecutable"] = builder.builderConfig.deploymentConfig.applicationInfo.exeName
        data["appIcon"] = builder.builderConfig.deploymentConfig.applicationInfo.mac.icons[0].target
        data["appId"] = builder.builderConfig.deploymentConfig.applicationId
        data["bundleFqdn"] = builder.builderConfig.deploymentConfig.applicationId
        data["appVersion"] = builder.builderConfig.deploymentConfig.version
        data["installerDir"] = builder.installerDir.absolutePathString()
        data["appSafeName"] = safeAppName(builder.builderConfig.deploymentConfig.applicationInfo.name)
        data["macOutputDir"] = builder.macDir.absolutePathString()
        val dirSizeBytes = FileUtils.sizeOfDirectory(builder.macDir.toFile())
        data["sizeInMb"] = (dirSizeBytes / mbBytes) + 2
        return data
    }

    private fun createInfoPlist() {
        val data = getTemplateData()
        val template = builder.freemarkerConfig.getTemplate(infoPlistTemplate)
        val targetPath = macOsDir.parent.resolve(infoPlistFile)
        val writer = FileWriter(targetPath.toFile())
        writer.use {
            template.process(data, writer)
        }
    }

    // Mac OS bundle signing fails if there are non-executable files in Content/MacOS directory.
    // this function moves them
    private fun moveNonExecutableFiles() {
        Files.list(macOsDir).forEach {
            if (it.isDirectory()) {
                try {
                    FileUtils.moveDirectory(it.toFile(), resourcesDir.resolve(it.fileName).toFile())
                    createSymlink(macOsDir.resolve(it.fileName), macOsDir.relativize(resourcesDir.resolve(it.fileName)))
                } catch(ex : IOException) {
                    logger.error("Failed to move directory", ex)
                }
            } else if (!it.isExecutable() && !it.isSymbolicLink()) {
                Files.move(it, resourcesDir.resolve(it.fileName))
                Files.createSymbolicLink(macOsDir.resolve(it.fileName), macOsDir.relativize(resourcesDir.resolve(it.fileName)))
            }

        }
    }

    private fun createSymlink(source : Path, target : Path) {
        logger.debug("Creating symlink: {} -> {}", source.absolutePathString(), target.toString())
        Files.createSymbolicLink(source, target)
    }

    private fun createInstaller() {
        logger.info("Creating Mac OS X installer")
        val dsStore = this.javaClass.getResourceAsStream(dsStoreInput)
        Files.copy(dsStore, builder.installerDir.resolve(dsStoreFile))
        val bgImage = this.javaClass.getResourceAsStream(backgroundImgInput)
        Files.copy(bgImage, builder.installerDir.resolve(backgroundImageFile))
        val data = getTemplateData()
        val template = builder.freemarkerConfig.getTemplate(createDmgTemplate)
        val installerFile = builder.installerDir.resolve(createDmgScriptFileName)
        val writer = FileWriter(installerFile.toFile())
        writer.use {
            template.process(data, writer)
        }
        installerFile.toFile().setExecutable(true)
        // invoke installer creation
        val pb = ProcessBuilder("./$createDmgScriptFileName")
        pb.directory(builder.installerDir.toFile())
        pb.environment().put("PWD", builder.installerDir.absolutePathString())
        logger.debug("create-dmg.sh: working directory={}", builder.installerDir.absolutePathString())
        val process = pb.start()
        if (process.waitFor() == 0) {
            logger.info(process.inputReader().use { it.readText() })
            logger.info("Mac OS DMG image created successfully")
        } else {
            logger.error("Error encountered while creating Mac OS DMG image. Details:")
            logger.error(process.inputReader().use { it.readText() })
            logger.error(process.errorReader().use { it.readText() })
            throw DeploymentException("Failed to build Mac DMG image")
        }
    }
}