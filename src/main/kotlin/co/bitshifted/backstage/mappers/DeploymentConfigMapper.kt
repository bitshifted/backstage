/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.mappers

import co.bitshifted.backstage.model.DeploymentConfig
import co.bitshifted.ignite.common.dto.DeploymentDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface DeploymentConfigMapper {

    @Mapping(target = "deploymentId", ignore = true)
    fun mapToDeploymentConfig(input : DeploymentDTO) : DeploymentConfig
}