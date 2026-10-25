/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.status

import com.reposilite.status.api.ConnectionPoolStatus
import com.reposilite.status.api.ResourcePools
import com.reposilite.status.api.ThreadPoolStatus
import com.reposilite.web.HttpServer
import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor

internal class ResourceUsageService(
    private val webServer: HttpServer,
    private val ioService: ExecutorService,
    private val databaseSource: HikariDataSource
) {

    fun fetchResourcePools(): ResourcePools =
        ResourcePools(
            web = webServer.fetchThreadPoolStatus(),
            io = (ioService as? ThreadPoolExecutor)?.let { pool ->
                ThreadPoolStatus(
                    active = pool.activeCount,
                    capacity = pool.maximumPoolSize,
                    queued = pool.queue.size,
                    reserved = 0
                )
            },
            database = databaseSource.hikariPoolMXBean?.let { pool ->
                ConnectionPoolStatus(
                    active = pool.activeConnections,
                    capacity = databaseSource.maximumPoolSize,
                    waiting = pool.threadsAwaitingConnection
                )
            }
        )

}
