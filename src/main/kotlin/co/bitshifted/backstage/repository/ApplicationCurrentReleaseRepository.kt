/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.repository

import co.bitshifted.backstage.entity.ApplicationCurrentRelease
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ApplicationCurrentReleaseRepository : CrudRepository<ApplicationCurrentRelease, Long> {

    fun findByApplicationId(applicationId : String) : Optional<ApplicationCurrentRelease>
}