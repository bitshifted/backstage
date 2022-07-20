/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.model

import co.bitshifted.ignite.common.dto.JvmConfigurationDTO
import co.bitshifted.ignite.common.model.ApplicationInfo
import co.bitshifted.ignite.common.model.BasicResource
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeploymentConfig(
    var deploymentId : String?,
    val applicationId : String,
    @JsonProperty("application-info")
    val applicationInfo : ApplicationInfo,
    @JsonProperty("jvm")
    val jvmConfiguration : JvmConfigurationDTO,
    val version : String,
    var resources : List<BasicResource> = mutableListOf()
)

