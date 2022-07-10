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

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class JavaVersion(@JsonValue val major : String) {
    JAVA_8("8"),
    JAVA_11("11"),
    JAVA_17("17")
}