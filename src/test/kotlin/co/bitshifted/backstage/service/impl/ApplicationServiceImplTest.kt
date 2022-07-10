/*
 *
 *  * Copyright (c) 2022-2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service.impl

import co.bitshifted.backstage.repository.ApplicationRepository
import co.bitshifted.backstage.entity.Application
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`


class ApplicationServiceImplTest {

    private val mockRepository = mock(ApplicationRepository::class.java)
    private  val applicationService = ApplicationServiceImpl(mockRepository)

    private val mockAppEntity = Application(name = "app-name", headline = "some headline", description = "app description", id = null)

    @Test
    fun createApplicationSuccess() {
        val result = Application(id = "some-id", name = mockAppEntity.name, headline = mockAppEntity.headline, description = mockAppEntity.description)
        `when`(mockRepository.save(mockAppEntity)).thenReturn(result)

        val out = applicationService.createApplication(mockAppEntity)
        assertEquals(result.id, out.id)
        assertEquals(result.name, out.name)
        assertEquals(result.headline, out.headline)
        assertEquals(result.description, out.description)
    }
}