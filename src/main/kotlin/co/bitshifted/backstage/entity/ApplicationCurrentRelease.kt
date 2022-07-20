/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package co.bitshifted.backstage.entity

import org.hibernate.annotations.GenericGenerator
import javax.persistence.*

@Entity
@Table(name = "current_release")
class ApplicationCurrentRelease (
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    var id : Long?,
    @Column(name = "application_id")
    var applicationId : String?,
    @Column(name = "release_id")
    var releaseId : String?
        )