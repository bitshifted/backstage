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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString

class WindowsDeploymentBuilder(val builder: DeploymentBuilder) {

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
            logger.info("Successfully created Windows deployment in directory {}", builder.windowsDir)
            return true
        } catch (th: Throwable) {
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

        var exeName = builder.config.deployment.applicationInfo.exeName
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
        builder.config.deployment.applicationInfo.windows.icons.forEach {
            val name = if (it.target != null) it.target else it.source
            val target = builder.windowsDir.resolve(name)
            logger.debug("Icon target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.config.contentService?.get(it.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM))
                .use {
                    Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
                }
        }
    }

    private fun copySplashScreen() {
        val splash = builder.config.deployment.applicationInfo.splashScreen
        if (splash!= null) {
            val name = if (splash.target != null) splash.target else splash.source
            val target = builder.windowsDir.resolve(name)
            logger.debug("Splash screen target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.config.contentService?.get(splash.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}