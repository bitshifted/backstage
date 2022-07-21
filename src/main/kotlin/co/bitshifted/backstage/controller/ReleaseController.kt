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

import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.service.ReleaseService
import co.bitshifted.ignite.common.model.OperatingSystem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Arrays

@RestController
@RequestMapping("/v1/releases")
class ReleaseController(@Autowired val releaseService: ReleaseService) {

    @GetMapping("/app/{appId}/current/{curReleaseId}/os/{os}", produces = [MediaType.APPLICATION_XML_VALUE])
    fun checkLatestRelease(@PathVariable("appId") applicationId : String, @PathVariable("curReleaseId") currentRelease : String, @PathVariable("os") os : String) : ResponseEntity<String> {
        val releaseInfo = releaseService.checkForNewRelease(applicationId, currentRelease, getOsFromString(os))
        return if (releaseInfo.isEmpty) {
            ResponseEntity.status(HttpStatus.NOT_MODIFIED).body("")
        } else {
            ResponseEntity.ok(releaseInfo.get())
        }
    }

    private fun getOsFromString(input : String) : OperatingSystem {
        return Arrays.stream(OperatingSystem.values()).filter { it.display == input }.findFirst().orElseThrow{ BackstageException(ErrorInfo.INVALID_INPUT, "os")}
    }
}