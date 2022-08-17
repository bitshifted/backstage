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

import co.bitshifted.ignite.common.dto.ApplicationDTO
import co.bitshifted.backstage.mappers.applicationMapper
import co.bitshifted.backstage.service.ApplicationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/applications")
class ApplicationController (
    @Autowired val applicationService: ApplicationService) {

    @PostMapping
    fun createApplication(@RequestBody application : ApplicationDTO) : ResponseEntity<ApplicationDTO> {
        val mapper = applicationMapper()
        val out = applicationService.createApplication(mapper.fromDto(application))
        return ResponseEntity.ok(mapper.toDto(out))
    }

    @GetMapping("/{id}")
    fun getApplication(@PathVariable("id") appId : String) : ResponseEntity<ApplicationDTO> {
        val out = applicationService.getApplication(appId)
        return ResponseEntity.ok(applicationMapper().toDto(out))
    }
}