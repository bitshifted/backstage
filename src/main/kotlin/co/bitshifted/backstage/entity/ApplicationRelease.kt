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

import co.bitshifted.backstage.util.GENERATOR_STRATEGY_NAME
import org.hibernate.annotations.GenericGenerator
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "app_release")
class ApplicationRelease(
    @Id
    @GenericGenerator(name = "id_generator", strategy = GENERATOR_STRATEGY_NAME)
    @GeneratedValue(generator = "id_generator")
    @Column(name = "release_id")
    var releaseId : String?,
    @Column(name = "application_id")
    var applicationId : String?,
    @Column(name = "deployment_id")
    var deploymentId : String,
    @Column(name = "release_timestamp")
    var releaseTimestamp : String?,
    @Column(name = "app_version")
    var version : String?
) {

}