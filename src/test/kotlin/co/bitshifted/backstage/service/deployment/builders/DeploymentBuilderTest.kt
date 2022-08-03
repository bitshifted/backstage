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

import co.bitshifted.backstage.model.DeploymentConfig
import co.bitshifted.backstage.service.ContentService
import co.bitshifted.backstage.util.safeAppName
import co.bitshifted.ignite.common.dto.JvmConfigurationDTO
import co.bitshifted.ignite.common.model.AppAuthor
import co.bitshifted.ignite.common.model.ApplicationInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.FileWriter
import java.nio.file.Files
import kotlin.io.path.absolutePathString

class DeploymentBuilderTest {

    @Test
    fun testValidInstallerTemplate() {
        val tmpDir = Files.createTempDirectory("template-test")
        val mockContentService = Mockito.mock(ContentService::class.java)
        val appInfo = ApplicationInfo()
        appInfo.name = "app-name"
        appInfo.exeName = "app-exe"
        appInfo.headline = "app headline"
        val deploymentConfig = DeploymentConfig(deploymentId = "123", applicationId = "appId",
            applicationInfo = appInfo, jvmConfiguration = JvmConfigurationDTO(), version = "1.2.3", resources = emptyList())
        val config = DeploymentBuilderConfig(tmpDir, deploymentConfig, mockContentService)

        val data = mutableMapOf<String, Any>()
        data["exe"] = config.deploymentConfig.applicationInfo.exeName
        data["appName"] = config.deploymentConfig.applicationInfo.name
        data["comment"] = config.deploymentConfig.applicationInfo.headline
        data["appSafeName"] = safeAppName(config.deploymentConfig.applicationInfo.name)
        data["version"] = config.deploymentConfig.version
        data["exeFiles"] = listOf("path/1", "/some/path2")
        data["appUrl"] = "https:///www.example.com"
        val author = AppAuthor()
        author.name = "john doe"
        author.email = "johndoe@mail.com"
        author.website = "http://example.com"
        data["authors"] = listOf(author)
        val builder = DeploymentBuilder(config)
        val template = builder.freemarkerConfig.getTemplate("linux/izpack-install.xml.ftl")
        val target = tmpDir.resolve("install-test.xml")
        println("target file: ${target.absolutePathString()}")
        val writer = FileWriter(target.toFile())
        writer.use {
            Assertions.assertDoesNotThrow { template.process(data, writer) }
        }

    }
}