/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service.impl

import co.bitshifted.backstage.BackstageConstants
import co.bitshifted.backstage.exception.DeploymentException
import co.bitshifted.ignite.common.model.JavaVersion
import co.bitshifted.ignite.common.model.JvmVendor
import co.bitshifted.backstage.service.ResourceMapping
import co.bitshifted.backstage.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.notExists

@Service
class FileSystemResourceMapping(@Value("\${jdk.root.location}") val jdkRoot : String) : ResourceMapping {

    val logger = logger(this)

    override fun getJdkLocation(vendor: JvmVendor, version: JavaVersion, exact: String): URI {
        val base = Path.of(jdkRoot, vendor.code, version.display)
        if (base.notExists()) {
            throw DeploymentException("Directory ${base.toFile().absolutePath} does not exist")
        }
        val target : Path
        if (exact != "") {
            target = base.resolve(exact)
        } else {
            target = base.resolve(BackstageConstants.LATEST_JAVA_DIR_LINK)
        }
        if (target.notExists()) {
            throw DeploymentException("Directory ${target.toFile().absolutePath} does not exist")
        }
        return target.toUri()
    }
}