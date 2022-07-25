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
import co.bitshifted.ignite.common.model.JavaVersion
import co.bitshifted.ignite.common.model.JvmVendor
import co.bitshifted.ignite.common.model.OperatingSystem
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileAttribute
import kotlin.io.path.absolutePathString

class FileSystemResourceMappingTest {

    @Test
    fun testGetJdkLocation() {
        val jdkRoot = Files.createTempDirectory("backstage-test")
        val jdkDir = Paths.get(jdkRoot.toAbsolutePath().toString(), JvmVendor.OPENJDK.code, JavaVersion.JAVA_11.display, OperatingSystem.LINUX.display, "11.0.2")
        Files.createDirectories(jdkDir)
        val latest = jdkDir.parent.resolve("latest")
        Files.createSymbolicLink(latest, jdkDir)

        val launchcodeRoot = Files.createTempDirectory("backstage-test-launchcode")
        val syncroJarLocation = Files.createTempFile("backstage-syncro", ".jar")

        val mapping = FileSystemResourceMapping(jdkRoot.toAbsolutePath().toString(), launchcodeRoot.absolutePathString(), syncroJarLocation.absolutePathString())
        val result = mapping.getJdkLocation(JvmVendor.OPENJDK, JavaVersion.JAVA_11, OperatingSystem.LINUX)
        Assertions.assertEquals(Path.of(result).toFile().absolutePath, latest.toFile().absolutePath)
        deleteDirectory(jdkRoot.toFile())
    }
}