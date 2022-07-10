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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class FileSystemContentServiceTest {

    var contentStorageLocation : String = ""

    @BeforeEach
    fun setup() {
       contentStorageLocation = Files.createTempDirectory("backstage").toFile().absolutePath
    }

    @AfterEach
    fun cleanup() {
        val file = File(contentStorageLocation)
        file.delete()
    }

    @Test
    fun saveContentSuccess() {
        val service = FileSystemContentService(contentStorageLocation)
        val fileUrl = javaClass.getResource("/content/content.txt")
        val file = File(fileUrl.toURI())
        val out = service.save(file.inputStream())
        assertEquals(
            Path.of(contentStorageLocation, "29", "0f", "49", "290f493c44f5d63d06b374d0a5abd292fae38b92cab2fae5efefe1b0e9347f56").toFile().absolutePath, out.path)
        val content = Files.readString(Path.of(out))
        assertEquals("some content", content)
    }

    @Test
    fun getContentSuccess() {
        val service = FileSystemContentService(contentStorageLocation)
        val fileStream = javaClass.getResourceAsStream("/content/content.txt")

        val out = service.save(fileStream)
        val hash = out.path.substring(out.path.lastIndexOf("/") + 1)
        val input = service.get(hash)
        val text = String(input.readAllBytes(), StandardCharsets.UTF_8)
        assertEquals("some content", text)
    }

    @Test
    fun confirmContentExists() {
        val service = FileSystemContentService(contentStorageLocation)
        val fileStream = javaClass.getResourceAsStream("/content/content.txt")
        val out = service.save(fileStream)
        val hash = out.path.substring(out.path.lastIndexOf("/") + 1)

        val result = service.exists(hash, 12)
        assertTrue(result)
    }
}