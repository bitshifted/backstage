/*
 *
 *  * Copyright (c) 2022-2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.appforge.backstage.service.deployment

import co.bitshifted.appforge.backstage.BackstageConstants
import co.bitshifted.appforge.backstage.exception.BackstageException
import co.bitshifted.appforge.backstage.exception.DeploymentException
import co.bitshifted.appforge.backstage.exception.ErrorInfo
import co.bitshifted.ignite.common.model.DeploymentStatus
import co.bitshifted.appforge.backstage.repository.DeploymentRepository
import co.bitshifted.appforge.backstage.util.logger
import co.bitshifted.appforge.backstage.util.maxThreadPoolSize
import co.bitshifted.appforge.backstage.util.threadPoolCoreSize
import org.apache.tomcat.util.threads.ThreadPoolExecutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.*

@Component
class DeploymentExecutorService(
    val taskMap : ConcurrentHashMap<FutureTask<*>, String> = ConcurrentHashMap(),
    @Autowired val deploymentRepository: DeploymentRepository) :
    ThreadPoolExecutor(
        threadPoolCoreSize(),
    maxThreadPoolSize(),
    BackstageConstants.THREAD_POOL_KEEP_ALIVE_MS,
    TimeUnit.MILLISECONDS,
    ArrayBlockingQueue(threadPoolCoreSize()) ) {

    val logger = logger(this)

    override fun beforeExecute(t: Thread?, r: Runnable?) {
        super.beforeExecute(t, r)
        if(taskMap.containsKey(r)) {
            val deploymentId = taskMap[r] ?: "unknown"
            logger.debug("Deployment ID: {}", deploymentId)
            val curDeployment = deploymentRepository.findById(deploymentId).orElseThrow { DeploymentException("Unknown deployment ID") }
            val curStatus = curDeployment.status
            logger.debug("Current deployment status: {}", curStatus)
            curDeployment.status = calculateNextStatus(curStatus)
            deploymentRepository.save(curDeployment)
            logger.debug("Updated status for deployment ID {} to {}", deploymentId, DeploymentStatus.STAGE_ONE_IN_PROGRESS)
        }
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
        if(taskMap.containsKey(r)) {
            val deploymentId = taskMap[r] ?: "unknown"
            logger.debug("Deployment ID: {}", deploymentId)
            if (t != null ) {
                val curDeployment = deploymentRepository.findById(deploymentId).orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, deploymentId) }
                curDeployment.status = DeploymentStatus.FAILED
                deploymentRepository.save(curDeployment)
                logger.debug("Updated status for deployment ID {} to {}", deploymentId, DeploymentStatus.FAILED)
                logger.error("Got exception:", t)
            }

        }
    }

    override fun submit(task: Runnable): Future<*> {
        val ftask = super.submit(task)
        if (task is DeploymentProcessTask && ftask is FutureTask) {
            taskMap[ftask] = task.taskConfig.deploymentConfig.deploymentId ?: "unknown"
        }
        return ftask
    }

    private fun calculateNextStatus(input : DeploymentStatus) : DeploymentStatus {
        return when(input) {
            DeploymentStatus.ACCEPTED -> DeploymentStatus.STAGE_ONE_IN_PROGRESS
            DeploymentStatus.STAGE_ONE_COMPLETED -> DeploymentStatus.STAGE_TWO_IN_PROGRESS
            else -> throw BackstageException(ErrorInfo.UNEXPECTED_DEPLOYMENT_STATUS, input.name)
        }
    }
}