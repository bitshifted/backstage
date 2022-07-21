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

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "release-info")
class ReleaseInfo(
    @get:XmlAttribute(name = "application-id") var applicationId : String? = null,
    @get:XmlAttribute(name = "release-id") var releaseId : String? = null,
    @get:XmlAttribute(name = "timestamp") var timestamp : String? = null,
    @get:XmlElement(name = "entry", type = ReleaseEntry::class) @get:XmlElementWrapper(name = "entries") var entries : List<ReleaseEntry>? = null
) {
}