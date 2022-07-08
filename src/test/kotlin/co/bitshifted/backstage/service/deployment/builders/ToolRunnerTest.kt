/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service.deployment.builders

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

class ToolRunnerTest {

    private lateinit var baseDir : Path

    @BeforeEach
    fun setup() {
        val baseDirPath = this.javaClass.getResource("/deployment-package")
        baseDir = Path.of(baseDirPath.toURI())
    }

    @Test
    fun getJdkModulesTest() {
        val runner = ToolsRunner(baseDir)
        val modules = runner.getJdkModules()
        assertTrue(modules.contains("java.base"))
        assertTrue(modules.contains("java.desktop"))
        assertFalse(modules.contains("foo"))
    }
}