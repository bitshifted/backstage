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

import co.bitshifted.ignite.common.dto.DeploymentDTO
import co.bitshifted.ignite.common.model.BasicResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest

inline fun <reified T> logger(from : T) : Logger {
    return LoggerFactory.getLogger(T::class.java)
}

fun threadPoolCoreSize() = Runtime.getRuntime().availableProcessors()

fun maxThreadPoolSize() = 2 * threadPoolCoreSize()

fun generateServerUrl(request: HttpServletRequest, path: String): String? {
    val sb = StringBuilder()
    sb.append(request.scheme).append("://")
    sb.append(request.serverName)
    if (request.serverPort != 0) {
        sb.append(":").append(request.serverPort)
    }
    sb.append(request.contextPath)
    if (!path.startsWith("/")) {
        sb.append("/")
    }
    sb.append(path)
    return sb.toString()
}

fun collectAllDeploymentResources(deployment : DeploymentDTO) : List<BasicResource> {
    val allResources = mutableListOf<BasicResource>()
    allResources.addAll(deployment.resources)
    if (deployment.applicationInfo.splashScreen != null) {
        allResources.add(deployment.applicationInfo.splashScreen)
    }
    if(deployment.applicationInfo.license != null) {
        allResources.add(deployment.applicationInfo.license)
    }

    allResources.addAll(deployment.applicationInfo.icons ?: emptyList())
    allResources.addAll(deployment.applicationInfo.linux.icons ?: emptyList())
    allResources.addAll(deployment.applicationInfo.mac.icons ?: emptyList())
    allResources.addAll(deployment.applicationInfo.windows.icons ?: emptyList())
    return allResources
}