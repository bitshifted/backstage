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

import co.bitshifted.backstage.dto.BasicResourceDTO
import co.bitshifted.backstage.dto.JvmDependencyDTO
import co.bitshifted.backstage.dto.RequiredResourcesDTO
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.model.DeploymentTaskConfig
import co.bitshifted.backstage.model.DeploymentStage
import co.bitshifted.backstage.model.DeploymentStatus
import co.bitshifted.backstage.repository.DeploymentRepository
import co.bitshifted.backstage.service.ContentService
import co.bitshifted.backstage.util.Downloader
import co.bitshifted.backstage.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import co.bitshifted.backstage.BackstageConstants.DEPLOYMENT_DEPENDENCIES_DIR
import co.bitshifted.backstage.BackstageConstants.DEPLOYMENT_OUTPUT_DIR
import co.bitshifted.backstage.BackstageConstants.DEPLOYMENT_RESOURCES_DIR
import co.bitshifted.backstage.model.OperatingSystem
import co.bitshifted.backstage.service.deployment.builders.DeploymentBuilder
import java.nio.file.Files
import java.nio.file.Path

const val MAVEN_CENTRAL_REPO_BASE_URL = "https://repo.maven.apache.org/maven2"

class DeploymentProcessTask  (
    val deploymentConfig: DeploymentTaskConfig
): Runnable {

    private val logger = logger(this)
    private val objectMapper = ObjectMapper()
    @Autowired var contentService : ContentService? = null
    @Autowired var downloader: Downloader? = null
    @Autowired lateinit var deploymentRepository : DeploymentRepository

    override fun run() {
        logger.info("Start processing deployment {}", deploymentConfig.deploymentId)
        if (deploymentConfig.stage == DeploymentStage.STAGE_ONE) {
            runDeploymentStageOne()
        } else {
            runDeploymentStageTwo()
        }
    }

    private fun runDeploymentStageOne() {
        // download dependencies if not exist
        val requirements = RequiredResourcesDTO()
        deploymentConfig.deployment.jvmConfig?.dependencies?.forEach {
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
        deploymentConfig.deployment.resources.forEach {
            val exists = contentService?.exists(it.sha256 ?: "unknown", it.size ?: 0) ?: false
            if (!exists) {
                logger.info("Adding resource ${it.source} to missing resources list")
                requirements.resources.add(it)
            }
        }

        val deployment = deploymentRepository?.findById(deploymentConfig.deploymentId)?.orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, deploymentConfig.deploymentId) }
        val text = objectMapper.writeValueAsString(requirements)
        logger.debug("Stage one requirements for deployment ID {}: {}", deploymentConfig.deploymentId, text)
        deployment?.requiredData = text
        deployment?.status = DeploymentStatus.STAGE_ONE_COMPLETED
        deploymentRepository?.save(deployment)

    }

    private fun runDeploymentStageTwo() {
        logger.info("Starting deployment stage two...")
        processFinalContent()
        val linuxOutputDir = createDeploymentStructure(OperatingSystem.LINUX)
        val builder = DeploymentBuilder(linuxOutputDir, deploymentConfig.deployment, contentService)
        val success = builder.build()
        val deployment = deploymentRepository?.findById(deploymentConfig.deploymentId)?.orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, deploymentConfig.deploymentId) }
        deployment.status = if(success) DeploymentStatus.SUCCESS else DeploymentStatus.FAILED
        deploymentRepository?.save(deployment)
    }

    private fun processFinalContent() {
        logger.debug("Checking final dependencies...")
        deploymentConfig.deployment.jvmConfig?.dependencies?.forEach {
            logger.debug("Checking dependency {}:{}:{}", it.groupId, it.artifactId, it.version)
            val exists = contentService?.exists(it.sha256 ?: "unknown", it.size ?: 0) ?: false
            if (!exists) {
                // copy from deployment directory
                logger.debug("Dependency {}:{}:{} does not exist in storage, trying deployment directory...", it.groupId, it.artifactId, it.version)
                val depPath = Paths.get(deploymentConfig.contentPath?.toFile()?.absolutePath, DEPLOYMENT_DEPENDENCIES_DIR, it.sha256)
                if(depPath.exists()) {
                    logger.debug("Copying dependency to storage...")
                    contentService?.save(depPath.inputStream())
                    logger.debug("Successfully stored dependency {}:{}:{}", it.groupId, it.artifactId, it.version)
                } else {
                    logger.error("Required dependency {}:{}:{} not found in deployment directory", it.groupId, it.artifactId, it.version)
                }
            }
        }
        val resourceBasePath = Paths.get(deploymentConfig.contentPath?.toFile()?.absolutePath, DEPLOYMENT_RESOURCES_DIR)
        deploymentConfig.deployment.resources.forEach {
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

    private fun createDeploymentStructure(os : OperatingSystem) : Path {
        val outputDir = Paths.get(deploymentConfig.contentPath?.toFile()?.absolutePath, DEPLOYMENT_OUTPUT_DIR, os.title)
        Files.createDirectories(outputDir)
        logger.info("Created output directory {}", outputDir.toFile().absolutePath)
        return outputDir
    }
}