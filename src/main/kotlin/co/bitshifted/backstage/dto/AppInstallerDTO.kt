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

import co.bitshifted.ignite.common.model.OperatingSystem


data class AppInstallerDTO(
    val applicationId : String? = null,
    val operatingSystem: OperatingSystem? = null,
    val extension : String? = null,
    val fileName : String? = null,
    val fileHash : String? = null,
    val size : Long = 0) {
}