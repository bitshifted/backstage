/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service.impl

import co.bitshifted.backstage.BackstageConstants
import co.bitshifted.backstage.dto.AppInstallerDTO
import co.bitshifted.backstage.entity.ApplicationCurrentRelease
import co.bitshifted.backstage.entity.ApplicationRelease
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.DeploymentException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.model.DeploymentConfig
import co.bitshifted.backstage.model.ReleaseEntry
import co.bitshifted.backstage.model.ReleaseInfo
import co.bitshifted.backstage.repository.ApplicationCurrentReleaseRepository
import co.bitshifted.backstage.repository.ApplicationReleaseRepository
import co.bitshifted.backstage.service.ContentService
import co.bitshifted.backstage.service.ReleaseService
import co.bitshifted.backstage.util.logger
import co.bitshifted.ignite.common.model.OperatingSystem
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.digest.MessageDigestAlgorithms
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import kotlin.io.path.absolutePathString

@Service("defaultReleaseService")
class DefaultReleaseService(
    @Autowired val releaseRepository : ApplicationReleaseRepository,
    @Autowired val currentReleaseRepository : ApplicationCurrentReleaseRepository,
    @Autowired val contentService: ContentService,
    @Value("\${release.storage.location}") val releaseStorageLocation : String
) : ReleaseService {

    private val logger = logger(this)
    private val timestampFormatter = DateTimeFormatterBuilder().appendPattern("YYYYMMddHHmmss").toFormatter()
    private val installerExtensions = mapOf<OperatingSystem, String>(
        OperatingSystem.WINDOWS to "exe",
        OperatingSystem.MAC to "dmg",
        OperatingSystem.LINUX to "tar.gz"
    )
    private val mapper = ObjectMapper()
    val digester = DigestUtils(MessageDigestAlgorithms.SHA_256)
    val releaseInfoFileNamePattern = "release-info-%s.xml"

    override fun initRelease(deploymentConfig: DeploymentConfig): String {
        logger.info(
            "Initializing release for application ID {} and deployment {}",
            deploymentConfig.applicationId,
            deploymentConfig.deploymentId
        )
        val instant = ZonedDateTime.now(ZoneId.of("UTC"))
        val timestamp = timestampFormatter.format(instant)
        val release = ApplicationRelease(
            releaseId = null,
            applicationId = deploymentConfig.applicationId,
            deploymentId = deploymentConfig.deploymentId ?: throw DeploymentException("Deployment ID can not be empty"),
            releaseTimestamp = timestamp,
            version = deploymentConfig.version
        )
        return releaseRepository.save(release).releaseId ?: throw DeploymentException("Invalid release ID: null")
    }

    override fun completeRelease(baseDir: Path, deploymentConfig: DeploymentConfig, releaseID: String) {
        logger.info("Creating release for ID {}", releaseID)
        val release = releaseRepository.findById(releaseID).orElseThrow { DeploymentException("Could not find release with ID " + releaseID) }
        deploymentConfig.applicationInfo.supportedOperatingSystems.forEach {
            createReleaseInfoFile(baseDir, it, deploymentConfig.applicationId, release.releaseId ?: "unknown", release.releaseTimestamp ?: "unknown")
        }
        createInstallersFile(baseDir, deploymentConfig.applicationId, release.releaseId ?: "unknown", deploymentConfig.applicationInfo.supportedOperatingSystems)
        val currentRelease = currentReleaseRepository.findByApplicationId(deploymentConfig.applicationId).orElse(
            ApplicationCurrentRelease(null, deploymentConfig.applicationId, null)
        )
        currentRelease.releaseId = releaseID
        currentReleaseRepository.save(currentRelease)
    }

    override fun checkForNewRelease(applicationId: String, currentRelease: String, os : OperatingSystem): Optional<String> {
        logger.info("Checking new release for application ID {} and current release {}", applicationId, currentRelease)
        val latestRelease = currentReleaseRepository.findByApplicationId(applicationId).orElseThrow { BackstageException(ErrorInfo.RELEASE_NOT_FOUND, applicationId) }
        if(currentRelease == latestRelease.releaseId) {
            return Optional.empty()
        } else {
            val releaseInfoFile = Paths.get(releaseStorageLocation, applicationId, latestRelease.releaseId, String.format(releaseInfoFileNamePattern, os.display))
            val data = Files.readString(releaseInfoFile)
            return Optional.of(data)
        }
    }

    override fun getInstallersList(applicationId: String): List<AppInstallerDTO> {
        logger.info("Getting installers list for application ID {}", applicationId)
        val latestRelease = currentReleaseRepository.findByApplicationId(applicationId).orElseThrow { BackstageException(ErrorInfo.RELEASE_NOT_FOUND, applicationId) }
        val installersFile = Paths.get(releaseStorageLocation, applicationId, latestRelease.releaseId, BackstageConstants.OUTPUT_INSTALLERS_FILE)
        val out : List<AppInstallerDTO> = mapper.readValue(installersFile.toFile(), mapper.typeFactory.constructCollectionType(List::class.java, AppInstallerDTO::class.java))
        return out
    }

    override fun getInstallerData(applicationId: String, hash: String): AppInstallerDTO {
        val installersList = getInstallersList(applicationId)
        return installersList.first { it.fileHash.equals(hash) }
    }

    private fun createReleaseInfoFile(baseDir: Path, os : OperatingSystem, applicationId : String,  releaseID : String, timestamp : String) {
        logger.info("Creating release info file")
        val osTargetDir = baseDir.resolve(os.display)
        logger.debug("OS target directory: {}", osTargetDir.absolutePathString())
        val filesList = FileUtils.listFiles(osTargetDir.toFile(), null, true)
        val entries = filesList.map {
            val target = osTargetDir.relativize(it.toPath()).toString()
            val hash = digester.digestAsHex(it)
            ReleaseEntry(hash, target, it.canExecute())
        }

        val releaseInfoFile = Paths.get(releaseStorageLocation, applicationId, releaseID, String.format(releaseInfoFileNamePattern, os.display))
        Files.createDirectories(releaseInfoFile.parent)
        val releaseInfo = ReleaseInfo(applicationId, releaseID, timestamp, entries)
        val ctx = JAXBContext.newInstance(ReleaseInfo::class.java)
        val marshaller = ctx.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        marshaller.marshal(releaseInfo, releaseInfoFile.toFile())
        logger.info("Created release info XML file in {}", releaseInfoFile.absolutePathString())
    }

    private fun createInstallersFile(baseDir: Path, applicationId: String, releaseId : String, osList : Set<OperatingSystem>) {
        val installersDir = baseDir.resolve(BackstageConstants.DEPLOYMENT_INSTALLERS_DIR)
        logger.debug("Installers directory: {}", installersDir.absolutePathString())
        val installersList = mutableListOf<AppInstallerDTO>()

        osList.forEach {
            val installerFiles = FileUtils.listFiles(installersDir.toFile(), arrayOf( installerExtensions[it]), false)
            logger.debug("Installer files: {}", installerFiles)
           for(file in installerFiles) {
               installersList.add(getAppInstallerData(applicationId, it, file, installerExtensions[it] ?: ""))
           }
        }
        val installerFile = Paths.get(releaseStorageLocation, applicationId, releaseId, BackstageConstants.OUTPUT_INSTALLERS_FILE).toFile()
        mapper.writeValue(installerFile, installersList)
        logger.info("Wrote installers.json file at {}", installerFile.absolutePath)
    }

    private fun getAppInstallerData(applicationId: String, os: OperatingSystem, installerFile : File, extension : String) : AppInstallerDTO {
        logger.debug("Calculating hash for installer file {}", installerFile.absolutePath)
        val installerHash = digester.digestAsHex(installerFile)
        contentService.save(installerFile.inputStream())
        return AppInstallerDTO(applicationId = applicationId, operatingSystem =  os , extension =  extension,
            fileHash =  installerHash, fileName = installerFile.name, size = installerFile.length())
    }

}