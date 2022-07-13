/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.util

import co.bitshifted.ignite.common.dto.JavaDependencyDTO
import org.springframework.stereotype.Component
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


@Component
class Downloader (val client : HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {

    val logger = logger(this)

    fun downloadJavaDependency(repoUrl : String, dependency : JavaDependencyDTO) : Pair<InputStream, Int> {
        val url =generateDependencyUrl(repoUrl, dependency)
        logger.debug("Downloading dependency {}", url)
        val req = HttpRequest.newBuilder(URI(url)).GET().build()
        val response = client.send(req, HttpResponse.BodyHandlers.ofInputStream())
        return Pair(response.body(), response.statusCode())
    }

    private fun generateDependencyUrl(repoUrl : String, jvmDependencyDTO: JavaDependencyDTO) : String {
        val group = jvmDependencyDTO.groupId?.replace(Regex("\\."), "/")
        val sb = StringBuilder(jvmDependencyDTO.artifactId)
        sb.append("-").append(jvmDependencyDTO.version)
        if (jvmDependencyDTO.classifier != null) {
            sb.append("-").append(jvmDependencyDTO.classifier)
        }
        sb.append(".").append(jvmDependencyDTO.type)
        val fileName = sb.toString()
        return String.format("%s/%s/%s/%s/%s", repoUrl, group, jvmDependencyDTO.artifactId, jvmDependencyDTO.version, fileName)
    }
}