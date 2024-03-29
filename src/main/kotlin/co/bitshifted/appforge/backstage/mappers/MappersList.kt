/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.appforge.backstage.mappers

import org.mapstruct.factory.Mappers

fun applicationMapper() : ApplicationMapper = Mappers.getMapper(ApplicationMapper::class.java)

fun deploymentConfigMapper() : DeploymentConfigMapper = Mappers.getMapper(DeploymentConfigMapper::class.java)

fun installedJdkMapper() : InstalledJdkMapper = Mappers.getMapper(InstalledJdkMapper::class.java)

fun availableJdkMapper() : AvailableJdkMapper = Mappers.getMapper(AvailableJdkMapper::class.java)

fun jdkInstallTaskMapper() : JdkInstallTaskMapper = Mappers.getMapper(JdkInstallTaskMapper::class.java)
