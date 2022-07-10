/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service

import co.bitshifted.backstage.model.JavaVersion
import co.bitshifted.backstage.model.JvmVendor
import java.net.URI

interface ResourceMapping {

    fun getJdkLocation(vendor : JvmVendor, version : JavaVersion, exact : String = "") : URI
}