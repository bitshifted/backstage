/*
 *
 *  * Copyright (c) 2022-2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.appforge.backstage.service.deployment.builders

import co.bitshifted.appforge.backstage.model.DeploymentConfig
import co.bitshifted.appforge.backstage.service.ContentService
import java.nio.file.Path

data class DeploymentBuilderConfig(val baseDir : Path, val deploymentConfig : DeploymentConfig, val contentService: ContentService?){}
