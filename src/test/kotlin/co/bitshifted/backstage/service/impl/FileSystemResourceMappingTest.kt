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

import co.bitshifted.backstage.deleteDirectory
import co.bitshifted.backstage.model.JavaVersion
import co.bitshifted.backstage.model.JvmVendor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FileSystemResourceMappingTest {

    @Test
    fun testGetJdkLocation() {
        val jdkRoot = Files.createTempDirectory("backstage-test")
        val jdkDir = Paths.get(jdkRoot.toAbsolutePath().toString(), JvmVendor.OPENJDK.code, JavaVersion.JAVA_11.major, "11.0.2")
        Files.createDirectories(jdkDir)
        val latest = jdkDir.parent.resolve("latest")
        Files.createSymbolicLink(latest, jdkDir)

        val mapping = FileSystemResourceMapping(jdkRoot.toAbsolutePath().toString())
        val result = mapping.getJdkLocation(JvmVendor.OPENJDK, JavaVersion.JAVA_11)
        Assertions.assertEquals(Path.of(result).toFile().absolutePath, latest.toFile().absolutePath)
        deleteDirectory(jdkRoot.toFile())
    }
}