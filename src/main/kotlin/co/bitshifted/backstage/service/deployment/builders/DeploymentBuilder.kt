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
import co.bitshifted.backstage.BackstageConstants.OUTPUT_LAUNCHER_DIR
import co.bitshifted.backstage.BackstageConstants.OUTPUT_MODULES_DIR
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.DeploymentException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.service.ContentService
import co.bitshifted.backstage.service.ResourceMapping
import co.bitshifted.backstage.util.logger
import co.bitshifted.ignite.common.model.OperatingSystem
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import freemarker.template.Version
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.digest.MessageDigestAlgorithms
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.absolutePathString


open class DeploymentBuilder(val config: DeploymentBuilderConfig) {

    private val logger = logger(this)
    private val argumentsFilePath = "config/embed/args.txt"
    private val jvmOptsFilePath = "config/embed/jvmopts.txt"
    private val propsFilePath = "config/embed/jvmprops.txt"
    private val cpFilePath = "config/embed/classpath.txt"
    private val modulepathFilePath = "config/embed/modulepath.txt"
    private val jarFilePath = "config/embed/jar.txt"
    private val splashFilePath = "config/embed/splash.txt"
    private val moduleFilePath = "config/embed/module.txt"
    private val mainClassFilePath = "config/embed/mainclass.txt"
    private val winIconPath = "icons/launchcode.ico"

    val freemarkerConfig  = Configuration(Version(2,3,20))
    val digester = DigestUtils(MessageDigestAlgorithms.SHA_256)

    init {
       freemarkerConfig.defaultEncoding = "UTF-8"
        freemarkerConfig.locale = Locale.ENGLISH
        freemarkerConfig.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        freemarkerConfig.setClassForTemplateLoading(DeploymentBuilder::class.java, "/templates")
    }

    @Autowired
    lateinit var resourceMapping: ResourceMapping
    @Autowired
    lateinit var contentService: ContentService
    lateinit var launchCodeDir: Path
    lateinit var linuxDir: Path
    lateinit var windowsDir: Path
    lateinit var macDir: Path

    fun build(): Boolean {
        try {
            createDirectoryStructure()
            buildLaunchers()
            val linuxBuilder = LinuxDeploymentBuilder(this)
            linuxBuilder.build()
            cacheDeploymentFiles(linuxDir)
            val windowsBuilder = WindowsDeploymentBuilder(this)
            windowsBuilder.build()
            cacheDeploymentFiles(windowsDir)
            val macBuilder = MacDeploymentBuilder(this)
            macBuilder.build()
            cacheDeploymentFiles(macDir)
        } catch (ex: Throwable) {
            logger.error("Failed to build deployment", ex)
            return false
        }
        return true
    }

    private fun createDirectoryStructure() {
        logger.debug("Creating directory structure for deployment in {}", config.baseDir.absolutePathString())
        launchCodeDir = Files.createDirectories(Paths.get(config.baseDir.absolutePathString(), OUTPUT_LAUNCHER_DIR))
        logger.debug("Created Launchcode output directory at {}", launchCodeDir.absolutePathString())
        linuxDir = Files.createDirectories(Paths.get(config.baseDir.absolutePathString(), OperatingSystem.LINUX.display))
        logger.debug("Created Linux output directory at {}", linuxDir.absolutePathString())
        windowsDir =
            Files.createDirectories(Paths.get(config.baseDir.absolutePathString(), OperatingSystem.WINDOWS.display))
        logger.debug("Created Windows output directory at {}", windowsDir.absolutePathString())
        macDir = Files.createDirectories(Paths.get(config.baseDir.absolutePathString(), OperatingSystem.MAC.display))
        logger.debug("Created Mac OS X output directory at {}", macDir.absolutePathString())
    }

    fun copyDependencies(modulesDir: Path, classpathDir: Path, os : OperatingSystem) {
        config.deployment.jvmConfiguration?.collectDependencies(os)?.forEach {
            var targetDIr: Path
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

    fun copyResources(baseDir: Path) {
        config.deployment.resources.forEach {
            val target = baseDir.resolve(it.target)
            logger.debug("Resource target: {}", target.toFile().absolutePath)
            // create directory structure
            Files.createDirectories(target.parent)
            config.contentService?.get(it.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    fun buildJdkImage(baseDir: Path, modulesDir: Path, os : OperatingSystem) {
        logger.info("Building JDK image")
        val jvmConfig =
            config.deployment.jvmConfiguration ?: throw DeploymentException("Can not find JVm configuration")
        val jdkLocation =
            resourceMapping.getJdkLocation(jvmConfig.vendor, jvmConfig.majorVersion, os, jvmConfig.fixedVersion ?: "")
        val moduleDirs = listOf(Path.of(jdkLocation).resolve(BackstageConstants.JDK_JMODS_DIR_NAME), modulesDir)
        val jreOutputDir = baseDir.resolve(BackstageConstants.OUTPUT_JRE_DIR)
        val toolRunner = ToolsRunner(baseDir)
        val jdkModules = toolRunner.getJdkModules()
        logger.debug("JDK modules to include: {}", jdkModules)
        if (jdkModules.isEmpty()) {
            throw DeploymentException("Failed to get any JDK module")
        }
        toolRunner.createRuntimeImage(jdkModules, moduleDirs, jreOutputDir)
    }

    private fun buildLaunchers() {
        val sourceRoot = resourceMapping.getLaunchcodeSourceLocation()
        logger.debug("Launchcode source location: {}", sourceRoot.toString())
        logger.debug("Copying Launchcode source to {}", launchCodeDir.absolutePathString())
        FileUtils.copyDirectory(File(sourceRoot), launchCodeDir.toFile())
        logger.debug("Finished copying Launchcode source")
        setupLauncherConfig()
        val pb = ProcessBuilder("make", "all")
        pb.directory(File(launchCodeDir.absolutePathString()))
        val path = System.getenv("PATH")
        pb.environment().put("PATH", "/usr/bin:/usr/local/bin:/usr/local/go/bin:/home/vlada/go/bin:/bin:/sbin")
        pb.environment().put("PWD", launchCodeDir.absolutePathString())

        println("PATH: $path")
        val process = pb.start()
        if (process.waitFor() == 0) {
            logger.info(process.inputReader().use { it.readText() })
            logger.info("Launchers created successfully")
        } else {
            logger.error("Error encountered while building launchers. Details:")
            logger.error(process.inputReader().use { it.readText() })
            logger.error(process.errorReader().use { it.readText() })
            throw DeploymentException("Failed to build launchers")
        }
    }

    private fun setupLauncherConfig() {
        logger.info("Configuring launcher")
        val argsPath = launchCodeDir.resolve(argumentsFilePath)
        Files.writeString(argsPath, config.deployment.jvmConfiguration.arguments ?: "")
        val jvmOptsPath = launchCodeDir.resolve(jvmOptsFilePath)
        Files.writeString(jvmOptsPath, config.deployment.jvmConfiguration.jvmOptions ?: "")
        val propsFile = launchCodeDir.resolve(propsFilePath)
        Files.writeString(propsFile, config.deployment.jvmConfiguration.systemProperties ?: "")
        val cpFilePath = launchCodeDir.resolve(cpFilePath)
        Files.writeString(cpFilePath, OUTPUT_CLASSPATH_DIR)
        val modulesPathDir = launchCodeDir.resolve(modulepathFilePath)
        Files.writeString(modulesPathDir, OUTPUT_MODULES_DIR)
        val jarPath = launchCodeDir.resolve(jarFilePath)
        Files.writeString(jarPath, config.deployment.jvmConfiguration.jar ?: "")
        if (config.deployment.applicationInfo.splashScreen != null) {
            val splashPath = launchCodeDir.resolve(splashFilePath)
            Files.writeString(splashPath, "-splash:${config.deployment.applicationInfo.splashScreen.target ?: config.deployment.applicationInfo.splashScreen.source}")
        }
        val modulePath = launchCodeDir.resolve(moduleFilePath)
        Files.writeString(modulePath, config.deployment.jvmConfiguration.moduleName ?: "")
        val mainClassPath = launchCodeDir.resolve(mainClassFilePath)
        Files.writeString(mainClassPath, config.deployment.jvmConfiguration.mainClass ?: "")
        // copy windows icon
        val winIcons = config.deployment.applicationInfo.windows.icons
        if (winIcons.size > 0) {
            val icon = winIcons[0]
            val iconTarget = launchCodeDir.resolve(winIconPath)
            logger.debug("Windows icon target: {}", iconTarget.absolutePathString())
            config.contentService?.get(icon.sha256 ?: throw BackstageException(ErrorInfo.EMPTY_CONTENT_CHECKSUM)).use {
                Files.copy(it, iconTarget, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun cacheDeploymentFiles(baseDir : Path) {
        logger.info("Caching deployment files for directory {}", baseDir.absolutePathString())
        val fileList = FileUtils.listFiles(baseDir.toFile(), null, true)
        fileList.stream().forEach {
            val hash = digester.digestAsHex(it)
            val size = it.length()
            logger.debug("Checking if {} exists", it.absolutePath)
            if(contentService.exists(hash, size)) {
                logger.debug("File is already cached")

            } else {
                logger.debug("File {} is not cached, caching it", it.absolutePath)
                contentService.save(it.inputStream(), it.canExecute())
            }
        }
    }

}