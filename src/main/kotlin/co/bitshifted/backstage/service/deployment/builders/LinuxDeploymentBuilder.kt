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

import co.bitshifted.backstage.BackstageConstants.LAUNCHER_NAME_LINUX
import co.bitshifted.backstage.BackstageConstants.OUTPUT_CLASSPATH_DIR
import co.bitshifted.backstage.BackstageConstants.OUTPUT_LAUNCHER_DIST_DIR
import co.bitshifted.backstage.BackstageConstants.OUTPUT_MODULES_DIR
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.util.logger
import co.bitshifted.ignite.common.model.OperatingSystem
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString

class LinuxDeploymentBuilder(val builder : DeploymentBuilder) {

    private val desktopEntryTemplate = "desktop-entry.desktop.ftl"
    val logger = logger(this)
    lateinit var classpathDir : Path
    lateinit var modulesDir : Path

    fun build(): Boolean {
        logger.info("Creating Linux deployment in directory {}", builder.linuxDir)
        try {
            createDirectoryStructure()
            builder.copyDependencies(modulesDir, classpathDir, OperatingSystem.LINUX)
            builder.copyResources(builder.linuxDir)
            builder.buildJdkImage(builder.linuxDir, modulesDir, OperatingSystem.LINUX)
            copyLauncher()
            copyLinuxIcons()
            copySplashScreen()
            createDesktopEntry()
            logger.info("Successfully created Linux deployment in directory {}", builder.linuxDir)
            return true
        } catch (th: Throwable) {
            logger.error("Error building Linux deployment", th)
            throw th
        }
    }

    private fun createDirectoryStructure() {
        classpathDir = Files.createDirectories(Paths.get(builder.linuxDir.absolutePathString(), OUTPUT_CLASSPATH_DIR))
        logger.info("Created classpath directory at {}", classpathDir.toFile().absolutePath)
        modulesDir = Files.createDirectories(Paths.get(builder.linuxDir.absolutePathString(), OUTPUT_MODULES_DIR))
        logger.info("Created modules directory at {}", modulesDir.toFile().absolutePath)
    }

    private fun copyLauncher() {
        val launcherPath = Path.of(builder.launchCodeDir.absolutePathString(), OUTPUT_LAUNCHER_DIST_DIR, LAUNCHER_NAME_LINUX)
        logger.debug("Copying Linux launcher from {} to {}", launcherPath.absolutePathString(), builder.linuxDir.resolve(LAUNCHER_NAME_LINUX))
        Files.copy(launcherPath, builder.linuxDir.resolve(builder.config.deployment.applicationInfo.exeName), StandardCopyOption.COPY_ATTRIBUTES)
    }

    private fun copyLinuxIcons() {
        builder.config.deployment.applicationInfo.linux.icons.forEach {
            val name = if (it.target != null) it.target else it.source
            val target = builder.linuxDir.resolve(name)
            logger.debug("Icon target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.config.contentService?.get(it.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun copySplashScreen() {
        val splash = builder.config.deployment.applicationInfo.splashScreen
        if (splash!= null) {
            val name = if (splash.target != null) splash.target else splash.source
            val target = builder.linuxDir.resolve(name)
            logger.debug("Splash screen target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.config.contentService?.get(splash.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun createDesktopEntry() {
        val data = mutableMapOf<String, String>()
        data["icon"] = builder.config.deployment.applicationInfo.linux.icons[0].target
        data["exe"] = builder.config.deployment.applicationInfo.exeName
        data["appName"] = builder.config.deployment.applicationInfo.name
        data["comment"] = builder.config.deployment.applicationInfo.headline
        val template = builder.freemarkerConfig.getTemplate(desktopEntryTemplate)
        val targetPath = builder.linuxDir.resolve("${builder.config.deployment.applicationInfo.exeName}.desktop")
        val writer = FileWriter(targetPath.toFile())
        writer.use {
            template.process(data, writer)
        }
    }
}