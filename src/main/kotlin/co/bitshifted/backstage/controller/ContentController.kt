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
import co.bitshifted.backstage.service.ContentService
import co.bitshifted.backstage.util.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

@RestController
@RequestMapping("/v1/content")
class ContentController(@Autowired val contentService: ContentService) {

    private val logger = logger(this)

    @GetMapping("/{hash}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getContent(@PathVariable("hash") hash : String) : ResponseEntity<ByteArray> {
        try {
            val bytes = contentService.get(hash).readAllBytes()
            return ResponseEntity.ok(bytes)
        } catch(th : Throwable) {
            logger.error("Failed to get content", th)
            throw BackstageException(ErrorInfo.INVALID_INPUT)
        }

    }
}