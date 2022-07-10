/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.dto

import co.bitshifted.backstage.model.JavaVersion
import co.bitshifted.backstage.model.JvmVendor
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JvmConfigurationDTO(
   @JsonProperty("dependencies") var dependencies : List<JvmDependencyDTO>? = null,
    @JsonProperty("vendor") var vendor : JvmVendor = JvmVendor.ADOPTIUM,
    @JsonProperty("major-version")  var majorVersion : JavaVersion = JavaVersion.JAVA_8,
    @JsonProperty("fixed-version") var fixedVersion : String? = null


)
