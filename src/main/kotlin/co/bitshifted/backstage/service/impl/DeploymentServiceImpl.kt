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
import co.bitshifted.ignite.common.dto.DeploymentDTO
import co.bitshifted.ignite.common.dto.DeploymentStatusDTO
import co.bitshifted.ignite.common.dto.RequiredResourcesDTO
import co.bitshifted.backstage.entity.Deployment
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.mappers.deploymentConfigMapper
import co.bitshifted.backstage.model.DeploymentConfig
import co.bitshifted.backstage.model.DeploymentStage
import co.bitshifted.ignite.common.model.DeploymentStatus
import co.bitshifted.backstage.model.DeploymentTaskConfig
import co.bitshifted.backstage.repository.ApplicationRepository
import co.bitshifted.backstage.repository.DeploymentRepository
import co.bitshifted.backstage.service.DeploymentService
import co.bitshifted.backstage.service.deployment.DeploymentExecutorService
import co.bitshifted.backstage.service.deployment.DeploymentProcessTask
import co.bitshifted.backstage.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream


@Service("deploymentServiceImpl")
class DeploymentServiceImpl(
    @Autowired val deploymentRepository: DeploymentRepository,
    @Autowired val applicationRepository: ApplicationRepository,
    @Autowired val deploymentTaskFactory : java.util.function.Function<DeploymentTaskConfig, DeploymentProcessTask>,
    @Autowired val deploymentExecutorService: DeploymentExecutorService,
    @Autowired val objectMapper: ObjectMapper) : DeploymentService {

    private val logger = logger(this)

    override fun submitDeployment(deploymentDto: DeploymentDTO): String? {
        val app = applicationRepository.findById(deploymentDto.applicationId).orElseThrow { BackstageException(ErrorInfo.NON_EXISTENT_APPLICATION_ID, deploymentDto.applicationId) }
        val deployment = Deployment(application = app, status = DeploymentStatus.ACCEPTED)
        val out = deploymentRepository.save(deployment)
        logger.info("Accepted deployment request with id {}", out.id)
        val deploymentConfig = deploymentConfigMapper().mapToDeploymentConfig(deploymentDto)
        deploymentConfig.deploymentId = out.id
        val taskConfig = DeploymentTaskConfig(deploymentConfig, DeploymentStage.STAGE_ONE)
        val deploymentProcessTask = deploymentTaskFactory.apply(taskConfig)
        deploymentExecutorService.submit(deploymentProcessTask)
        logger.info("Deployment ID {} submitted for processing ", out.id)
        return out.id
    }

    override fun submitDeploymentArchive(deploymentId: String, ins: InputStream): String? {
        // unpack deployment archive to temporary directory
        val tempDir = Files.createTempDirectory("backstage-")
        logger.debug("Created deployment directory {}", tempDir.toFile().absolutePath)
        var buff = ByteArray(4096)
        ZipInputStream(ins).use {
            var entry = it.nextEntry
            while (entry != null) {
                val entryPath = tempDir.resolve(entry.name)
                logger.debug("Entry target path: {}", entryPath)
                Files.createDirectories(entryPath.parent)

                val fos = FileOutputStream(entryPath.toFile())
                var len: Int
                while (it.read(buff).also { len = it } > 0) {
                    fos.write(buff, 0, len)
                }
                fos.close()
                entry = it.nextEntry
            }
        }
        val deploymentFile = tempDir.resolve(BackstageConstants.DEPLOYMENT_CONFIG_FILE)
        val deploymentConfig = objectMapper.readValue(deploymentFile.toFile(), DeploymentConfig::class.java)
        deploymentConfig.deploymentId = deploymentId
        val app = applicationRepository.findById(deploymentConfig.applicationId).orElseThrow { BackstageException(ErrorInfo.NON_EXISTENT_APPLICATION_ID, deploymentConfig.applicationId) }
        deploymentConfig.applicationInfo.name = app.name
        deploymentConfig.applicationInfo.headline = app.headline
        deploymentConfig.applicationInfo.description = app.description
        deploymentConfig.applicationInfo.homePageUrl = app.homePageUrl
        deploymentConfig.applicationInfo.publisher = app.publisher
        deploymentConfig.applicationInfo.publisherEmail = app.publisherEmail
        val taskConfig = DeploymentTaskConfig(deploymentConfig, DeploymentStage.STAGE_TWO, tempDir)
        val deploymentProcessTask = deploymentTaskFactory.apply(taskConfig)
        deploymentExecutorService.submit(deploymentProcessTask)
        logger.info("Deployment ID {} submitted for processing ", deploymentId)
        return deploymentId
    }

    override fun getDeployment(deploymentId: String): DeploymentStatusDTO {
        val deployment = deploymentRepository.findById(deploymentId).orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, deploymentId) }
        var text = deployment.requiredData
        if(text == null || text == "") {
            text = "{}"
        }
        return DeploymentStatusDTO(deployment.status, objectMapper.readValue(text, RequiredResourcesDTO::class.java))
    }
}