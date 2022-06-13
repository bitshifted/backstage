/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.service.deployment

import co.bitshifted.backstage.BackstageConstants
import co.bitshifted.backstage.exception.BackstageException
import co.bitshifted.backstage.exception.ErrorInfo
import co.bitshifted.backstage.model.DeploymentStatus
import co.bitshifted.backstage.repository.DeploymentRepository
import co.bitshifted.backstage.util.logger
import co.bitshifted.backstage.util.maxThreadPoolSize
import co.bitshifted.backstage.util.threadPoolCoreSize
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
            val curDeployment = deploymentRepository.findById(deploymentId).orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, deploymentId) }
            curDeployment.status = DeploymentStatus.STAGE_ONE_IN_PROGRESS
            deploymentRepository.save(curDeployment)
            logger.debug("Updated status for deployment ID {} to {}", deploymentId, DeploymentStatus.STAGE_ONE_IN_PROGRESS)
        }
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
        if(taskMap.containsKey(r)) {
            val deploymentId = taskMap[r] ?: "unknown"
            logger.debug("Deployment ID: {}", deploymentId)
            val curDeployment = deploymentRepository.findById(deploymentId).orElseThrow { BackstageException(ErrorInfo.DEPLOYMENT_NOT_FOND, deploymentId) }
            val newStatus = if (t != null) DeploymentStatus.FAILED else DeploymentStatus.SUCCESS
            curDeployment.status = newStatus
            deploymentRepository.save(curDeployment)
            logger.debug("Updated status for deployment ID {} to {}", deploymentId, newStatus)
        }
    }

    override fun submit(task: Runnable): Future<*> {
        val ftask = super.submit(task)
        if (task is DeploymentProcessTask && ftask is FutureTask) {
            taskMap[ftask] = task.deploymentConfig.id
        }
        return ftask
    }
}