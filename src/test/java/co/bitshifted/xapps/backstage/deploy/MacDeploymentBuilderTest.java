/*
 * Copyright (c) 2020. Bitshift (http://bitshifted.co)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package co.bitshifted.xapps.backstage.deploy;

import co.bitshifted.xapps.backstage.content.ContentMapping;
import co.bitshifted.xapps.backstage.deploy.builders.DeploymentBuilder;
import co.bitshifted.xapps.backstage.deploy.builders.MacDeploymentBuilder;
import co.bitshifted.xapps.backstage.model.*;
import co.bitshifted.xapps.backstage.test.TestConfig;
import co.bitshifted.xapps.backstage.util.PackageUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Function;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vladimir Djurovic
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class MacDeploymentBuilderTest {

	private static final String TEST_ARCHIVE_NAME = "7PjPKl4iLX7.zip";

	@Autowired
	private ContentMapping contentMapping;
	@Autowired
	private Function<TargetDeploymentInfo, DeploymentBuilder> deploymentBuilderFactory;

	private Path deploymentWorkDir;
	private Path deploymentPackageDir;
	private DeploymentBuilder deploymentBuilder;
	private DeploymentConfig deploymentConfig;
	private Path jdkLinkPath;

	@Before
	public void setup() throws Exception {
		var workspace = contentMapping.getWorkspaceUri();
		var dirName = "7PjPKl4iLX7_20200201-131415-123";
		deploymentWorkDir = Path.of(workspace).resolve(dirName);
		var archivePath = Path.of(ToolsRunnerTest.class.getResource("/deployment/7PjPKl4iLX7.zip").toURI());
		Files.createDirectory(deploymentWorkDir);
		var copyTarget = deploymentWorkDir.resolve(TEST_ARCHIVE_NAME);
		Files.copy(archivePath, copyTarget, StandardCopyOption.REPLACE_EXISTING);
		deploymentPackageDir = PackageUtil.unpackZipArchive(copyTarget);
		// copy dummy launcher
		var dummyLauncherPath = Path.of(getClass().getResource("/launchcode-mac-x64").toURI());
		var launcherTarget = Path.of(contentMapping.getLauncherStorageUri()).resolve("launchcode-mac-x64");
		Files.copy(dummyLauncherPath, launcherTarget, StandardCopyOption.REPLACE_EXISTING);

		var launcherConfig = new LauncherConfig();
		launcherConfig.setVersion("1.0.0");
		var jvm = new JvmConfig();
		jvm.setMainClass("my.MainClass");
		jvm.setJvmDir("/some/jvm/dir");
		launcherConfig.setJvm(jvm);

		var configBuilder = DeploymentConfig.builder();
		configBuilder.appName("TestApp")
				.appId("appId")
				.appVersion("1.0.0")
				.icons(List.of(
						new FileInfo("icon1.png","data/icons/icon1.png"),
						new FileInfo("winicon.ico","data/icons/winicon.ico"),
						new FileInfo("maicon.icns", "data/icons/maicon.icns")))
				.splashScreen(new FileInfo("splash.png", "data/splash.png"))
				.jdkProvider(JdkProvider.OPENJDK)
				.jvmImplementation(JvmImplementation.HOTSPOT)
				.jdkVersion(JdkVersion.JDK_11)
		.launcherConfig(launcherConfig);
		deploymentConfig = configBuilder.build();

		var targetDeploymentInfo = TargetDeploymentInfo.builder()
				.deploymentConfig(deploymentConfig)
				.deploymentPackageDir(deploymentPackageDir)
				.targetOs(OS.MAC_OS_X)
				.targetCpuArch(CpuArch.X_64)
				.build();


		deploymentBuilder = deploymentBuilderFactory.apply(targetDeploymentInfo);
		// create link to system JDK
		var javaHome = System.getProperty("java.home");
		var jdkStorageDirPath = Path.of(contentMapping.getJdkStorageUri());
		jdkLinkPath = jdkStorageDirPath.resolve("openjdk-hotspot-11-linux-x64");
		Files.createSymbolicLink(jdkLinkPath, Path.of(javaHome));
	}

	@After
	public void cleanup() throws Exception {
		Files.deleteIfExists(jdkLinkPath);
		FileUtils.deleteDirectory(deploymentWorkDir.toFile());
	}


	@Test
	@Ignore
	public void testMacDeploymentBuild() throws Exception {
		var request = mock(HttpServletRequest.class);
		when(request.getScheme()).thenReturn("http");
		when(request.getServerName()).thenReturn("my.server.host");
		when(request.getServerPort()).thenReturn(8000);
		when(request.getContextPath()).thenReturn("");
		deploymentBuilder.createDeployment();
	}
}
