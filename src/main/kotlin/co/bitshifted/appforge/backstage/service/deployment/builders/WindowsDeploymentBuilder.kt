/*
 *
 *  * Copyright (c) 2022-2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.appforge.backstage.service.deployment.builders

import co.bitshifted.appforge.backstage.BackstageConstants
import co.bitshifted.appforge.backstage.exception.BackstageException
import co.bitshifted.appforge.backstage.exception.DeploymentException
import co.bitshifted.appforge.backstage.exception.ErrorInfo
import co.bitshifted.appforge.backstage.util.logger
import co.bitshifted.appforge.backstage.util.safeAppName
import co.bitshifted.appforge.common.model.OperatingSystem
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString

class WindowsDeploymentBuilder(val builder: DeploymentBuilder) {

    private val installerTemplate = "windows/installer.nsi.ftl"
    private val installerConfigFileName = "installer.nsi"
    private val nsisCompilerCmd = "makensis"
    val logger = logger(this)
    lateinit var classpathDir: Path
    lateinit var modulesDir: Path

    fun build(): Boolean {
        logger.info("Creating Windows deployment in directory {}", builder.windowsDir)
        try {
            createDirectoryStructure()
            builder.copyDependencies(modulesDir, classpathDir, OperatingSystem.WINDOWS)
            builder.copyResources(builder.windowsDir)
            builder.buildJdkImage(builder.windowsDir, modulesDir, OperatingSystem.WINDOWS)
            copyLauncher()
            copyWindowsIcons()
            copySplashScreen()
            createInstaller()
            logger.info("Successfully created Windows deployment in directory {}", builder.windowsDir)
            return true
        } catch (th: Throwable) {
            logger.error("Error building Windows deployment", th)
            throw th
        }
    }

    private fun createDirectoryStructure() {
        classpathDir = Files.createDirectories(
            Paths.get(
                builder.windowsDir.absolutePathString(),
                BackstageConstants.OUTPUT_CLASSPATH_DIR
            )
        )
        logger.info("Created classpath directory at {}", classpathDir.toFile().absolutePath)
        modulesDir = Files.createDirectories(
            Paths.get(
                builder.windowsDir.absolutePathString(),
                BackstageConstants.OUTPUT_MODULES_DIR
            )
        )
        logger.info("Created modules directory at {}", modulesDir.toFile().absolutePath)
    }

    private fun copyLauncher() {
        val launcherPath = Path.of(
            builder.launchCodeDir.absolutePathString(),
            BackstageConstants.OUTPUT_LAUNCHER_DIST_DIR,
            BackstageConstants.LAUNCHER_NAME_WINDOWS
        )

        var exeName = builder.builderConfig.deploymentConfig.applicationInfo.exeName
        if (!exeName.endsWith(".exe")) {
            exeName = "$exeName.exe"
        }
        logger.debug(
            "Copying Windows launcher from {} to {}", launcherPath.absolutePathString(), builder.windowsDir.resolve(
                exeName
            )
        )
        Files.copy(
            launcherPath,
            builder.windowsDir.resolve(exeName),
            StandardCopyOption.COPY_ATTRIBUTES
        )
    }

    private fun copyWindowsIcons() {
        builder.builderConfig.deploymentConfig.applicationInfo.windows.icons.forEach {
            val name = if (it.target != null) it.target else it.source
            val target = builder.windowsDir.resolve(name)
            logger.debug("Icon target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.builderConfig.contentService?.get(it.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM))
                .use {
                    Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
                }
        }
    }

    private fun copySplashScreen() {
        val splash = builder.builderConfig.deploymentConfig.applicationInfo.splashScreen
        if (splash!= null) {
            val name = if (splash.target != null) splash.target else splash.source
            val target = builder.windowsDir.resolve(name)
            logger.debug("Splash screen target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.builderConfig.contentService?.get(splash.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun getTemplateData() : MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>()
        data["exe"] = builder.builderConfig.deploymentConfig.applicationInfo.exeName
        data["appName"] = builder.builderConfig.deploymentConfig.applicationInfo.name
        data["appSafeName"] = safeAppName(builder.builderConfig.deploymentConfig.applicationInfo.name)
        data["version"] = builder.builderConfig.deploymentConfig.version
        data["licenseFile"] = builder.windowsDir.resolve(builder.builderConfig.deploymentConfig.applicationInfo.license.target).absolutePathString()
        data["contentDir"] = builder.windowsDir.absolutePathString()
        val installerExeName = String.format("%s-%s-windows.exe", safeAppName(builder.builderConfig.deploymentConfig.applicationInfo.name), builder.builderConfig.deploymentConfig.version)
        data["installerExe"] = builder.installerDir.resolve(installerExeName).absolutePathString()
        return data
    }

    private fun createInstaller() {
        logger.info("Creating installer in directory {}", builder.builderConfig.baseDir.absolutePathString())
        val data = getTemplateData()
        val template = builder.freemarkerConfig.getTemplate(installerTemplate)
        val installerFile = builder.builderConfig.baseDir.resolve(installerConfigFileName)
        val writer = FileWriter(installerFile.toFile())
        writer.use {
            template.process(data, writer)
        }
        // run NSIS compiler
        val pb = ProcessBuilder(nsisCompilerCmd, installerConfigFileName)
        pb.directory(builder.builderConfig.baseDir.toFile())
        pb.environment().put("PWD", builder.builderConfig.baseDir.absolutePathString())
        logger.debug("makensis: working directory={}", builder.builderConfig.baseDir.absolutePathString())
        val process = pb.start()
        if (process.waitFor() == 0) {
            logger.info(process.inputReader().use { it.readText() })
            logger.info("NSIS installer created successfully")
        } else {
            logger.error("Error encountered while NSIS installer. Details:")
            logger.error(process.inputReader().use { it.readText() })
            logger.error(process.errorReader().use { it.readText() })
            throw DeploymentException("Failed to build NSIS installer")
        }
        // make installer executable
        val installerExe = Path.of(data["installerExe"].toString()).toFile()
        installerExe.setExecutable(true)
    }
}