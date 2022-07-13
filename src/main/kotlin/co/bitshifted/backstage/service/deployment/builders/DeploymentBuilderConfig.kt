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

import co.bitshifted.ignite.common.dto.DeploymentDTO
import co.bitshifted.backstage.service.ContentService
import java.nio.file.Path

data class DeploymentBuilderConfig(val baseDir : Path, val deployment : DeploymentDTO, val contentService: ContentService?){}