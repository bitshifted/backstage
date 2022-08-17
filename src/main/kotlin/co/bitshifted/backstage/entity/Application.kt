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
import javax.persistence.*

@Entity(name = "application")
@Table(name = "application")
class Application (
    @Id
    @GenericGenerator(name = "id_generator", strategy = GENERATOR_STRATEGY_NAME)
    @GeneratedValue(generator = "id_generator")
    @Column(name = "app_id")  var id : String?,
    @Column(name = "app_name") var name : String,
    var headline : String? = null,
    @Column(name = "app_description") var description : String? = null,
    @Column(name = "home_page_url") var homePageUrl : String? = null,
    var publisher : String? = null,
    @Column(name ="publisher_email") var publisherEmail : String? = null
        ) {
}