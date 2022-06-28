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

import co.bitshifted.backstage.dto.DeploymentDTO
import co.bitshifted.backstage.dto.DeploymentStatusDTO
import co.bitshifted.backstage.dto.RequiredResourcesDTO
import co.bitshifted.backstage.entity.Deployment
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.mappers.deploymentMapper
import co.bitshifted.backstage.model.DeploymentTaskConfig
import co.bitshifted.backstage.model.DeploymentStage
import co.bitshifted.backstage.model.DeploymentStatus
import co.bitshifted.backstage.repository.ApplicationRepository
import co.bitshifted.backstage.repository.DeploymentRepository
import co.bitshifted.backstage.service.DeploymentService
import co.bitshifted.backstage.service.deployment.DeploymentExecutorService
import co.bitshifted.backstage.service.deployment.DeploymentProcessTask
import co.bitshifted.backstage.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream

@Service("deploymentServiceImpl")
class DeploymentServiceImpl(
    @Autowired val deploymentRepository: DeploymentRepository,
    @Autowired val applicationRepository: ApplicationRepository,
    @Autowired val deploymentTaskFactory : java.util.function.Function<DeploymentTaskConfig, DeploymentProcessTask>,
    @Autowired val deploymentExecutorService: DeploymentExecutorService) : DeploymentService {

    val logger = logger(this)
    val objectMapper = ObjectMapper()

    override fun submitDeployment(deploymentDto: DeploymentDTO): String? {
        val app = applicationRepository.findById(deploymentDto.applicationId).orElseThrow { BackstageException(ErrorInfo.NON_EXISTENT_APPLICATION_ID, deploymentDto.applicationId) }
        val deployment = Deployment(application = app, status = DeploymentStatus.ACCEPTED)
        val out = deploymentRepository.save(deployment)
        logger.info("Accepted deployment request with id {}", out.id)

        val taskConfig = deploymentMapper().dtoToDeploymentTaskConfig(deploymentDto, out.id ?: "", DeploymentStage.STAGE_ONE)
        val deploymentProcessTask = deploymentTaskFactory.apply(taskConfig)
        deploymentExecutorService.submit(deploymentProcessTask)
        logger.info("Deployment ID {} submitted for processing ", out.id)
        return out.id
    }

    override fun submitDeploymentArchive(deploymentId: String, ins: InputStream): String? {
        val deployment = deploymentRepository.findById(deploymentId).orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, deploymentId) }

        val taskConfig = deploymentMapper().dtoToDeploymentTaskConfig(deploymentDto, out.id ?: "", DeploymentStage.STAGE_ONE)
        return "dummy"
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