/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.appforge.backstage.service

import co.bitshifted.ignite.common.model.JavaVersion
import co.bitshifted.ignite.common.model.JvmVendor
import co.bitshifted.ignite.common.model.OperatingSystem
import java.net.URI

interface ResourceMapping {

    fun getJdkLocation(vendor : JvmVendor, version : JavaVersion, os : OperatingSystem, exact : String = "") : URI

    fun getLaunchcodeSourceLocation() : URI

    fun getSyncroJarLocation() : URI
}