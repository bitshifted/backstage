/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.config

import co.bitshifted.backstage.model.DeploymentTaskConfig
import co.bitshifted.backstage.service.deployment.DeploymentProcessTask
import co.bitshifted.backstage.service.deployment.builders.DeploymentBuilder
import co.bitshifted.backstage.service.deployment.builders.DeploymentBuilderConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.util.function.Function

@Configuration
class DeploymentConfig {

    @Bean
    @Scope("prototype")
    fun deploymentProcessTask(deploymentConfig : DeploymentTaskConfig) : DeploymentProcessTask =  DeploymentProcessTask(deploymentConfig)

    @Bean
    fun deploymentTaskFactory(): Function<DeploymentTaskConfig, DeploymentProcessTask>? {
        return Function { source: DeploymentTaskConfig ->
            deploymentProcessTask(source)
        }
    }

    @Bean
    @Scope("prototype")
    fun deploymentBuilder(config : DeploymentBuilderConfig) : DeploymentBuilder = DeploymentBuilder(config)

    @Bean
    fun deploymentBuilderFactory() : Function<DeploymentBuilderConfig, DeploymentBuilder> {
        return Function {
            source : DeploymentBuilderConfig -> deploymentBuilder(source)
        }
    }
}