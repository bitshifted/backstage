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

import com.fasterxml.jackson.annotation.JsonProperty

data class ApplicationInfoDTO(
    @JsonProperty("splash-screen") var splashScreen : BasicResourceDTO?,
    @JsonProperty("icons") var icons : List<BasicResourceDTO>?,
    @JsonProperty("windows") var windows : ApplicationInfoDTO?,
    @JsonProperty("linux") var linux : ApplicationInfoDTO?,
    @JsonProperty("mac") var mac : ApplicationInfoDTO?
)
