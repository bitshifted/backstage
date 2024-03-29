/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.appforge.backstage.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FunctionsTests {

    @Test
    fun testExtractGzip() {
        val archivePathUrl = this.javaClass.getResource("/testarchive.tar.gz")
        val archivePath = Path.of(archivePathUrl.toURI())
        val tmpDir = Files.createTempDirectory("tar_gz_unpack_test_")
        val out = extractTarGzArchive(archivePath, tmpDir, logger(this))
        val expected = tmpDir.resolve("archive")
        assertEquals(expected.absolutePathString(), out?.absolutePathString())
    }

    @Test
    fun testExtractZip() {
        val archivePathUrl = this.javaClass.getResource("/testarchive.zip")
        val archivePath = Path.of(archivePathUrl.toURI())
        val tmpDir = Files.createTempDirectory("zip_unpack_test_")
        val out = extractZipArchive(archivePath, tmpDir, logger(this))
        val expected = tmpDir.resolve("archive")
        assertEquals(expected.absolutePathString(), out?.absolutePathString())
    }

    @Test
    fun cleanArchiveEntryPathTest() {
        var input = "./entry/foo/"
        var result = cleanArchiveEntryPath(input)
        assertEquals("entry/foo", result)

        input = "foo/bar"
        result = cleanArchiveEntryPath(input)
        assertEquals(input, result)
    }
}