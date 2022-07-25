/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.config

import co.bitshifted.ignite.common.dto.JavaDependencyDTO
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration

//@Configuration
@ConstructorBinding
@ConfigurationProperties(prefix = "syncro")
data class SyncroConfig(val groupId : String, val artifactId : String, val version : String) {

    fun toJavaDependency(hash : String) : JavaDependencyDTO {
        val dto = JavaDependencyDTO()
        dto.groupId = groupId
        dto.artifactId = artifactId
        dto.version = version
        dto.isModular = false
        dto.sha256 = hash
        return dto
    }
}
