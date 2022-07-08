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

import co.bitshifted.backstage.BackstageConstants
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.util.logger
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.spi.ToolProvider
import kotlin.io.path.name

class ToolsRunner(val buildDir: Path) {

    private val logger = logger(this)
    private val jdeps = ToolProvider.findFirst("jdeps").orElseThrow()
    private val jlink = ToolProvider.findFirst("jlink").orElseThrow()

    fun getJdkModules(): Set<String> {
        val outString = StringWriter();
        val out = PrintWriter(outString);
        val errString = StringWriter();
        val err = PrintWriter(errString);

        val argsList = mutableListOf<String>()
        argsList.add("--module-path")
        argsList.add(buildDir.resolve(BackstageConstants.OUTPUT_MODULES_DIR).toFile().absolutePath)
        argsList.add("--list-deps")
        argsList.addAll(getAllJarsInDirectory(buildDir.resolve(BackstageConstants.OUTPUT_MODULES_DIR)))
        argsList.addAll(getAllJarsInDirectory(buildDir.resolve(BackstageConstants.OUTPUT_CLASSPATH_DIR)))
        logger.debug("jdeps arguments list: {}", argsList)
        val result = jdeps.run(out, err, *argsList.toTypedArray())
        val modules = mutableSetOf<String>()
        if (result == 0) {
            logger.debug("jdeps output: {}", outString.toString());
            BufferedReader(StringReader(outString.toString())).useLines {
                it.filter { line -> isJdkModule(line.trim()) }.forEach { line -> modules.add(line.trim()) }
            }
        } else {
            logger.error("Error running jdeps: {}", errString.toString())
            throw BackstageException(ErrorInfo.TOOL_RUN_ERROR, "jdeps")
        }
        logger.debug("Found JDK modules: {}", modules)
        return modules
    }

    private fun isJdkModule(moduleName: String): Boolean {
        return BackstageConstants.JDK_MODULES_PREFIXES.any { moduleName.startsWith(it.trim()) }

    }

    private fun getAllJarsInDirectory(directory: Path): List<String> {
        return Files.list(directory).filter { it.name.endsWith(BackstageConstants.JAR_EXTENSION) }
            .map { it.toFile().absolutePath }.toList()
    }
}