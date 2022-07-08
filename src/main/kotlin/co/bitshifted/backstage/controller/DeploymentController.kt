/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.controller

import co.bitshifted.backstage.BackstageConstants
import co.bitshifted.backstage.dto.DeploymentDTO
import co.bitshifted.backstage.dto.DeploymentStatusDTO
import co.bitshifted.backstage.service.DeploymentService
import co.bitshifted.backstage.util.generateServerUrl
import co.bitshifted.backstage.util.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

const val BASE_PATH = "/v1/deployments"

@RestController
@RequestMapping(BASE_PATH)
class DeploymentController(
    @Autowired val deploymentService: DeploymentService) {

    val logger = logger(this)

    @PostMapping
    fun startDeployment(@RequestBody deployment : DeploymentDTO, request : HttpServletRequest) : ResponseEntity<String> {
        logger.debug("Running deployment stage one for application id {}", deployment.applicationId)
        val deploymentId = deploymentService.submitDeployment(deployment)
        val statusUrl = generateServerUrl(request, "$BASE_PATH/$deploymentId")

        return ResponseEntity.accepted().header(BackstageConstants.DEPLOYMENT_STATUS_HEADER, statusUrl).build()
    }

    @GetMapping("/{deploymentId}")
    fun getDeployment(@PathVariable deploymentId : String) : ResponseEntity<DeploymentStatusDTO> {
        val deployment = deploymentService.getDeployment(deploymentId)
        return ResponseEntity.ok(deployment)
    }

    @PutMapping(value = ["/{deploymentId}"], consumes = ["application/zip"])
    fun acceptDeploymentArchive(@PathVariable deploymentId: String, @RequestBody content : ByteArray, request : HttpServletRequest) : ResponseEntity<String> {
        logger.debug("Accepted deployment archive for {}", deploymentId)
        deploymentService.submitDeploymentArchive(deploymentId, content.inputStream())
        val statusUrl = generateServerUrl(request, "$BASE_PATH/$deploymentId")
        return ResponseEntity.accepted().header(BackstageConstants.DEPLOYMENT_STATUS_HEADER, statusUrl).build()
    }
}