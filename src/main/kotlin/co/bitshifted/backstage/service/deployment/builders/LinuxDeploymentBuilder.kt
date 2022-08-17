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
import co.bitshifted.backstage.util.directoryToTarGz
import co.bitshifted.backstage.util.logger
import co.bitshifted.backstage.util.safeAppName
import co.bitshifted.ignite.common.model.OperatingSystem
import org.apache.commons.io.FileUtils
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.stream.Collector
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString

class LinuxDeploymentBuilder(val builder : DeploymentBuilder) {

    private val desktopEntryTemplate = "linux/desktop-entry.desktop.ftl"
    private val installerTemplate = "linux/install-script.sh.ftl"
    private val installerFileName = "installer.sh"
    private val contentArchiveName = "content.tar.gz"
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
            createInstaller()
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
        Files.copy(launcherPath, builder.linuxDir.resolve(builder.builderConfig.deploymentConfig.applicationInfo.exeName), StandardCopyOption.COPY_ATTRIBUTES)
    }

    private fun copyLinuxIcons() {
        builder.builderConfig.deploymentConfig.applicationInfo.linux.icons.forEach {
            val name = if (it.target != null) it.target else it.source
            val target = builder.linuxDir.resolve(name)
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
            val target = builder.linuxDir.resolve(name)
            logger.debug("Splash screen target: {}", target.toFile().absolutePath)
            Files.createDirectories(target.parent)
            builder.builderConfig.contentService?.get(splash.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun getTemplateData() : MutableMap<String, Any> {
        logger.debug("Linux desktop categories: {}", builder.builderConfig.deploymentConfig.applicationInfo.linux.categories)
        val data = mutableMapOf<String, Any>()
        data["icon"] = builder.builderConfig.deploymentConfig.applicationInfo.linux.icons[0].target
        data["exe"] = builder.builderConfig.deploymentConfig.applicationInfo.exeName
        data["appName"] = builder.builderConfig.deploymentConfig.applicationInfo.name
        data["comment"] = builder.builderConfig.deploymentConfig.applicationInfo.headline
        data["appSafeName"] = safeAppName(builder.builderConfig.deploymentConfig.applicationInfo.name)
        data["categories"] = builder.builderConfig.deploymentConfig.applicationInfo.linux.categories.stream().collect(Collectors.joining(";"))
        logger.debug("template categories: {}", data["categories"])
        data["version"] = builder.builderConfig.deploymentConfig.version
        data["appUrl"] = builder.builderConfig.deploymentConfig.applicationInfo.homePageUrl ?: ""
        data["publisher"] = builder.builderConfig.deploymentConfig.applicationInfo.publisher
        // find all executable files
        val fileList = FileUtils.listFiles(builder.linuxDir.toFile(), null, true)
        val exeFiles = fileList.filter { it.canExecute() }.map { builder.linuxDir.relativize(it.toPath()).toString() }
        data["exeFiles"] = exeFiles
        return data
    }

    private fun createDesktopEntry() {
        val data = getTemplateData()
        val template = builder.freemarkerConfig.getTemplate(desktopEntryTemplate)
        val safeName = data["appSafeName"]
        val targetPath = builder.linuxDir.resolve("${safeName}.desktop")

        val writer = FileWriter(targetPath.toFile())
        writer.use {
            template.process(data, writer)
        }
    }

    private fun createInstaller() {
        logger.info("Creating installer in directory {}", builder.installerDir.absolutePathString())
        val data = getTemplateData()
        val installerWorkDirName = String.format("linux/%s-%s-linux", data["appSafeName"], data["version"])
        val workDir = builder.installerDir.resolve(installerWorkDirName)
        Files.createDirectories(workDir)
        logger.debug("Linux installer working directory: {}", workDir.absolutePathString())
        val template = builder.freemarkerConfig.getTemplate(installerTemplate)
        val installerFile = workDir.resolve(installerFileName)
        val writer = FileWriter(installerFile.toFile())
        writer.use {
            template.process(data, writer)
        }
        installerFile.toFile().setExecutable(true)
        // copy content
        val contentDir = workDir.resolve("content")
        Files.createDirectories(contentDir)
        FileUtils.copyDirectory(builder.linuxDir.toFile(), contentDir.toFile())
        val installerName = String.format("%s-%s-linux.tar.gz",  data["appSafeName"], data["version"])
        val installerPath = builder.installerDir.resolve(installerName)
        logger.debug("Linux installer name: {}", installerName)
        directoryToTarGz(workDir.parent, installerPath)
        // cleanup
        FileUtils.deleteDirectory(workDir.toFile())
    }
}