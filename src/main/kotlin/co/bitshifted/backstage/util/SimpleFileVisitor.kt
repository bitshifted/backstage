/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize

class SimpleFileVisitor(val baseDir : Path, val archiveOs : TarArchiveOutputStream) : FileVisitor<Path> {

    val logger = logger(this)
    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        if(attrs != null && attrs.isSymbolicLink) {
           return FileVisitResult.CONTINUE
       }
        val targetFile = baseDir.relativize(file)
        logger.debug("Source file: {}, target file: {}", file?.absolutePathString(), targetFile.toString())
        val entry = TarArchiveEntry(targetFile.toString())
        entry.size = Files.size(file)
        if(Files.isExecutable(file)) {
            entry.mode = 0x0755
        }
        archiveOs.putArchiveEntry(entry)
        archiveOs.write(file?.toFile()?.readBytes())
        archiveOs.closeArchiveEntry()
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
        logger.error("Failed to visit file {}", file?.absolutePathString())
        return FileVisitResult.TERMINATE
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

}