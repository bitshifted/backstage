/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.appforge.backstage.mappers

import co.bitshifted.appforge.backstage.model.jdk.JavaPlatformDetails
import co.bitshifted.appforge.common.dto.jdk.JavaPlatformInfoDTO
import org.mapstruct.Mapper

@Mapper(uses = [AvailableJdkReleaseMapper::class])
interface AvailableJdkMapper {

    fun toPlatformDetailsDTO(input : JavaPlatformDetails) : JavaPlatformInfoDTO
}