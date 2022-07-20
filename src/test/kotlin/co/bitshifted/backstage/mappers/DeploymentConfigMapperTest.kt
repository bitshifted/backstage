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

import co.bitshifted.ignite.common.dto.DeploymentDTO
import co.bitshifted.ignite.common.dto.JvmConfigurationDTO
import co.bitshifted.ignite.common.model.ApplicationInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeploymentConfigMapperTest {

    @Test
    fun verifyDtoToConfigMapping() {
        val dto = DeploymentDTO()
        dto.applicationId = "app-id"
        dto.version = "1.2.3"

        val applicationInfo = ApplicationInfo()
        applicationInfo.name = "app name"
        applicationInfo.description = "description"
        dto.applicationInfo = applicationInfo
        dto.jvmConfiguration = JvmConfigurationDTO()

        val config = deploymentConfigMapper().mapToDeploymentConfig(dto)
        assertEquals("app-id", config.applicationId)
        assertEquals("1.2.3", config.version)
        assertNotNull(config.applicationInfo)
        assertEquals("app name", config.applicationInfo.name)
        assertEquals("description", config.applicationInfo.description)
    }
}