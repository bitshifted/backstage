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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class BasicResourceDTO(
    var source : String?,
    var target: String?,
    var sha256 : String?,
    var size : Long?
)
