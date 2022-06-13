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
import co.bitshifted.backstage.service.DeploymentService
import co.bitshifted.backstage.util.generateServerUrl
import co.bitshifted.backstage.util.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/v1/deployments")
class DeploymentController(
    @Autowired val deploymentService: DeploymentService) {

    val logger = logger(this)

    @PostMapping
    fun startDeployment(@RequestBody deployment : DeploymentDTO, request : HttpServletRequest) : ResponseEntity<String> {
        logger.debug("Running deployment stage one for application id {}", deployment.applicationId)
        val deploymentId = deploymentService.validateDeployment(deployment)
        val statusUrl = generateServerUrl(request, "/deployment/" + deploymentId)

        return ResponseEntity.accepted().header(BackstageConstants.DEPLOYMENT_STATUS_HEADER, statusUrl).build()
    }
}