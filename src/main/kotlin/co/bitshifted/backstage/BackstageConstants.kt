/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage

import java.time.ZoneId

object BackstageConstants {

    /**
     * UTC timezone ID.
     */
     val UTC_TIME_ZONE = ZoneId.of("UTC")

    val JDK_MODULES_PREFIXES = listOf("java.", "jdk.")

    /**
     * HTTP header containing URL of deployment status.
     */
    const val DEPLOYMENT_STATUS_HEADER = "X-Deployment-Status"

    const val THREAD_POOL_KEEP_ALIVE_MS = 500L

    const val DEPLOYMENT_DEPENDENCIES_DIR = "dependencies"
    const val DEPLOYMENT_RESOURCES_DIR = "resources"
    const val DEPLOYMENT_OUTPUT_DIR = "output"
    const val OUTPUT_CLASSPATH_DIR = "cp"
    const val OUTPUT_MODULES_DIR = "modules"
    const val JAR_EXTENSION = ".jar"
    const val JDK_JMODS_DIR_NAME = "jmods"
}