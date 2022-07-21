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
import co.bitshifted.backstage.service.ReleaseService
import co.bitshifted.backstage.util.logger
import co.bitshifted.ignite.common.model.OperatingSystem
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.digest.MessageDigestAlgorithms
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
    @Value("\${release.storage.location}") val releaseStorageLocation : String
) : ReleaseService {

    private val logger = logger(this)
    private val timestampFormatter = DateTimeFormatterBuilder().appendPattern("YYYYMMddHHmmss").toFormatter()
    val digester = DigestUtils(MessageDigestAlgorithms.SHA_256)
    val releaseInfoFileNamePattern = "release-info-%s.xml"

    override fun createRelease(baseDir: Path, deploymentConfig: DeploymentConfig) {
        logger.info("Creating release for application ID {}", deploymentConfig.applicationId)
        val instant = ZonedDateTime.now(ZoneId.of("UTC"))
        val timestamp = timestampFormatter.format(instant)
        val release = ApplicationRelease(releaseId = null, applicationId = deploymentConfig.applicationId,
            deploymentId = deploymentConfig.deploymentId ?: throw DeploymentException("Deployment ID can not be empty"), releaseTimestamp = timestamp, version = deploymentConfig.version)
        val releaseId = releaseRepository.save(release).releaseId ?: throw DeploymentException("Invalid release ID: null")
        deploymentConfig.applicationInfo.supportedOperatingSystems.forEach {
            createReleaseInfoFile(baseDir, it, deploymentConfig.applicationId, releaseId, timestamp)
        }
        val currentRelease = currentReleaseRepository.findByApplicationId(deploymentConfig.applicationId).orElse(
            ApplicationCurrentRelease(null, deploymentConfig.applicationId, null)
        )
        currentRelease.releaseId = releaseId
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

    private fun createReleaseInfoFile(baseDir: Path, os : OperatingSystem, applicationId : String,  releaseID : String, timestamp : String) {
        logger.info("Creating release info file")
        val osTargetDir = baseDir.resolve(os.display)
        logger.debug("OS target directory: {}", osTargetDir.absolutePathString())
        val filesList = FileUtils.listFiles(osTargetDir.toFile(), null, true)
        val entries = filesList.map {
            logger.debug("Current file: {}", it.absolutePath)
            val target = osTargetDir.relativize(it.toPath()).toString()
            logger.debug("Entry target: {}", target)
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

}