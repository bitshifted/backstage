/*
 *
 *  * Copyright (c) 2022-2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.controller

import co.bitshifted.backstage.repository.ApplicationRepository
import co.bitshifted.backstage.TestConfig
import co.bitshifted.ignite.common.dto.ApplicationDTO
import co.bitshifted.backstage.entity.Application
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.*

const val createApplicationPayload = """
    {"name": "app-name", "headline": "app headline", "description": "app description" }
"""

const val getApplicationResponsePayload = """
    {"id": "app-id", "name": "app-name", "headline": "app headline", "description": "app description" }
"""

@ExtendWith(SpringExtension::class)
@WebAppConfiguration
@ContextConfiguration(classes = [TestConfig::class])
class ApplicationControllerTest {

    val savedEntity = Application(id = "app-id", name = "app-name", headline = "app headline", description = "app description")

    private lateinit var mockMvc : MockMvc
    @Autowired
    private lateinit var applicationController: ApplicationController
    @Autowired
    private lateinit var applicationRepository: ApplicationRepository
    @Autowired
    private lateinit var webAppContext : WebApplicationContext
    @Autowired
    private lateinit var objectMapper : ObjectMapper

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build()
    }

    @Test
    fun createApplicationSuccess() {
        `when`(applicationRepository.save(any())).thenReturn(savedEntity)

        val result = mockMvc.perform(
            post("/v1/applications")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(createApplicationPayload)
        ).andReturn()
        assertEquals(HttpStatus.OK.value(), result.response.status)
        val output = objectMapper.readValue(result.response.contentAsString, ApplicationDTO::class.java)
        assertEquals(savedEntity.id, output.id)
        assertEquals(savedEntity.name, output.name)
        assertEquals(savedEntity.headline, output.headline)
        assertEquals(savedEntity.description, output.description)
    }

    @Test
    fun getApplicationSuccess() {
        `when`(applicationRepository.findById("app-id")).thenReturn(Optional.of(savedEntity))
        val result = mockMvc.perform(get("/v1/applications/app-id")).andReturn()
        assertEquals(HttpStatus.OK.value(), result.response.status)
    }

}