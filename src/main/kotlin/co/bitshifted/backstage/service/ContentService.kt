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

import java.io.InputStream
import java.net.URI
import java.net.URL

interface ContentService {

    fun save(input : InputStream) : URI

    fun save(input: InputStream, executable : Boolean)

    fun get(sha256: String) : InputStream

    fun exists(sha256 : String, size : Long) : Boolean
}