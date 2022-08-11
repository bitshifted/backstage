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

import co.bitshifted.backstage.dto.AppInstallerDTO
import co.bitshifted.backstage.service.ReleaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/applications")
class AppReleaseController(
    @Autowired val releaseService: ReleaseService) {

    @GetMapping("/{applicationId}/installers")
    fun getCurrentReleaseInstallers(@PathVariable("applicationId") applicationId : String) : ResponseEntity<List<AppInstallerDTO>> {
        return  ResponseEntity.ok( releaseService.getInstallersList(applicationId))
    }
}