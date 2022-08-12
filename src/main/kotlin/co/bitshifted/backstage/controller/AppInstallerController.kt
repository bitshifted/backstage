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
import co.bitshifted.backstage.service.ContentService
import co.bitshifted.backstage.service.ReleaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/applications")
class AppInstallerController(
    @Autowired val releaseService: ReleaseService,
    @Autowired val contentService: ContentService) {

    @GetMapping("/{applicationId}/installers")
    fun getCurrentReleaseInstallers(@PathVariable("applicationId") applicationId : String) : ResponseEntity<List<AppInstallerDTO>> {
        return  ResponseEntity.ok( releaseService.getInstallersList(applicationId))
    }

    @GetMapping("/{applicationId}/installers/{hash}")
    fun downloadInstaller(@PathVariable("applicationId") applicationId: String, @PathVariable("hash") fileHash : String) : ResponseEntity<ByteArray> {
        val bytes = contentService.get(fileHash).readAllBytes()
        val metadata = releaseService.getInstallerData(applicationId, fileHash)
        val contetnDisposition = ContentDisposition.builder("inline").filename(metadata.fileName ?: "unknown").build()
        val headers = HttpHeaders()
        headers.contentDisposition = contetnDisposition
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.contentLength = metadata.size
        return ResponseEntity.ok().headers(headers).body(bytes)
    }
}