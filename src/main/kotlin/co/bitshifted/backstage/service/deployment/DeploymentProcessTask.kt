/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service.deployment

import co.bitshifted.backstage.BackstageConstants.DEPLOYMENT_DEPENDENCIES_DIR
import co.bitshifted.backstage.BackstageConstants.DEPLOYMENT_OUTPUT_DIR
import co.bitshifted.backstage.BackstageConstants.DEPLOYMENT_RESOURCES_DIR
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.model.DeploymentStage
import co.bitshifted.ignite.common.model.DeploymentStatus
import co.bitshifted.backstage.model.DeploymentTaskConfig
import co.bitshifted.backstage.repository.DeploymentRepository
import co.bitshifted.backstage.service.ContentService
import co.bitshifted.backstage.service.deployment.builders.DeploymentBuilder
import co.bitshifted.backstage.service.deployment.builders.DeploymentBuilderConfig
import co.bitshifted.backstage.util.Downloader
import co.bitshifted.backstage.util.collectAllDeploymentResources
import co.bitshifted.backstage.util.logger
import co.bitshifted.ignite.common.dto.RequiredResourcesDTO
import co.bitshifted.ignite.common.model.DeploymentStatus.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream

const val MAVEN_CENTRAL_REPO_BASE_URL = "https://repo.maven.apache.org/maven2"

class DeploymentProcessTask  (
    val taskConfig: DeploymentTaskConfig
): Runnable {

    private val logger = logger(this)
    private val objectMapper = ObjectMapper()
    @Autowired var contentService : ContentService? = null
    @Autowired var downloader: Downloader? = null
    @Autowired lateinit var deploymentRepository : DeploymentRepository
    @Autowired lateinit var deploymentBuilderFactory : java.util.function.Function<DeploymentBuilderConfig, DeploymentBuilder>

    override fun run() {
        logger.info("Start processing deployment {}", taskConfig.deploymentConfig.deploymentId)
        try {
            if (taskConfig.stage == DeploymentStage.STAGE_ONE) {
                runDeploymentStageOne()
            } else {
                runDeploymentStageTwo()
            }
        } catch(th : Throwable) {
            logger.error("Failed to run deployment process", th)
        }

    }

    private fun runDeploymentStageOne() {
        // download dependencies if not exist
        val requirements = RequiredResourcesDTO()
        taskConfig.deploymentConfig.jvmConfiguration?.collectAllDependencies()?.forEach {
            val exists = contentService?.exists(it.sha256 ?: "unknown", it.size ?: 0) ?: false
            if (!exists) {
                logger.debug("Dependency {} does not exist", it.artifactId)
                val response = downloader?.downloadJavaDependency(MAVEN_CENTRAL_REPO_BASE_URL, it)
                if (response?.second != HttpStatus.OK.value()) {
                    logger.info("Added dependency to missing list")
                    requirements.dependencies.add(it)
                } else {
                    logger.info("Saving dependency {}", it.artifactId)
                    contentService?.save(response?.first)
                }
            }
        }
        // check for missing resources
        val allResources = collectAllDeploymentResources(taskConfig.deploymentConfig)
        allResources.forEach {
            val exists = contentService?.exists(it.sha256 ?: "unknown", it.size ?: 0) ?: false
            if (!exists) {
                logger.info("Adding resource ${it.source} to missing resources list")
                requirements.resources.add(it)
            }
        }

        val deployment = deploymentRepository?.findById(taskConfig.deploymentConfig.deploymentId ?: "unknown")?.orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, taskConfig.deploymentConfig.deploymentId) }
        val text = objectMapper.writeValueAsString(requirements)
        logger.debug("Stage one requirements for deployment ID {}: {}", taskConfig.deploymentConfig.deploymentId, text)
        deployment?.requiredData = text
        deployment?.status = STAGE_ONE_COMPLETED
        deploymentRepository?.save(deployment)

        logger.info("Deployment ID {}, status: {}", taskConfig.deploymentConfig.deploymentId, deployment.status)
    }

    private fun runDeploymentStageTwo() {
        logger.info("Starting deployment stage two...")
        setDeploymentStatus(STAGE_TWO_IN_PROGRESS)
        processFinalContent()
        val outputDir = createDeploymentStructure()
        val builder = deploymentBuilderFactory.apply(DeploymentBuilderConfig( outputDir, taskConfig.deploymentConfig, contentService))
        val success = builder.build()
        setDeploymentStatus( if(success) SUCCESS else FAILED)
    }

    private fun processFinalContent() {
        logger.debug("Checking final dependencies...")
        taskConfig.deploymentConfig.jvmConfiguration?.dependencies?.forEach {
            logger.debug("Checking dependency {}:{}:{}", it.groupId, it.artifactId, it.version)
            val exists = contentService?.exists(it.sha256 ?: "unknown", it.size ?: 0) ?: false
            if (!exists) {
                // copy from deployment directory
                logger.debug("Dependency {}:{}:{} does not exist in storage, trying deployment directory...", it.groupId, it.artifactId, it.version)
                val depPath = Paths.get(taskConfig.contentPath?.toFile()?.absolutePath, DEPLOYMENT_DEPENDENCIES_DIR, it.sha256)
                if(depPath.exists()) {
                    logger.debug("Copying dependency to storage...")
                    contentService?.save(depPath.inputStream())
                    logger.debug("Successfully stored dependency {}:{}:{}", it.groupId, it.artifactId, it.version)
                } else {
                    logger.error("Required dependency {}:{}:{} not found in deployment directory", it.groupId, it.artifactId, it.version)
                }
            }
        }
        val allResources = collectAllDeploymentResources(taskConfig.deploymentConfig)
        val resourceBasePath = Paths.get(taskConfig.contentPath?.toFile()?.absolutePath, DEPLOYMENT_RESOURCES_DIR)
        allResources.forEach {
            logger.debug("Checking resource {}, hash {}", it.target, it.sha256)
            val exists = contentService?.exists(it.sha256 ?: "unknown", it.size ?: 0) ?: false
            if (!exists) {
                logger.debug("Resource {} does not exist in storage, trying deployment directory")
                val resPath = resourceBasePath.resolve(it.target)
                if (resPath.exists()) {
                    logger.debug("Resource path {} exists. Copying resource to storage.", resPath.toFile().absolutePath)
                    contentService?.save(resPath.inputStream())
                    logger.debug("Resource {]  copied to storage", it.target)
                } else {
                    logger.error("Required resource {} not found in deployment directory", it.target)
                }
            }
        }
    }

    private fun createDeploymentStructure() : Path {
        val outputDir = Paths.get(taskConfig.contentPath?.toFile()?.absolutePath, DEPLOYMENT_OUTPUT_DIR)
        Files.createDirectories(outputDir)
        logger.info("Created output directory {}", outputDir.toFile().absolutePath)
        return outputDir
    }

    private fun setDeploymentStatus(status : DeploymentStatus) {
        val deployment = deploymentRepository?.findById(taskConfig.deploymentConfig.deploymentId ?: "unknown") ?. orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, taskConfig.deploymentConfig.deploymentId) }
        deployment.status = status
        deploymentRepository?.save(deployment)
    }
}