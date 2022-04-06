/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.settings.infrastructure

import com.reposilite.journalist.Journalist
import com.reposilite.settings.ConfigurationProvider
import com.reposilite.settings.ConfigurationRepository
import com.reposilite.shared.extensions.validateAndLoad
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFoundError
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import net.dzikoysk.cdn.Cdn
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.function.ThrowingFunction
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.createInstance

internal class SqlConfigurationProvider<C : Any>(
    override val name: String,
    override val displayName: String,
    private val journalist: Journalist,
    private val configurationRepository: ConfigurationRepository,
    override val configuration: C
) : ConfigurationProvider<C> {

    private val standard: Cdn = KCdnFactory.createStandard()
    private var databaseUpdateTime = Instant.ofEpochMilli(0)
    private var watcher: ScheduledFuture<*>? = null

    override fun initialize() {
        journalist.logger.info("Loading ${displayName.lowercase()} from database")
        loadRemoteConfiguration()
    }

    private fun loadRemoteConfiguration() {
        configurationRepository.findConfiguration(name)
            ?.let { standard.load(Source.of(it), configuration) }
            ?.peek { journalist.logger.info("$displayName has been loaded from database") }
            ?.orElseThrow(ThrowingFunction.identity())
            ?: standard.render(configuration)
                .map { configurationRepository.saveConfiguration(name, it) }
                .orElseThrow(ThrowingFunction.identity())

        this.databaseUpdateTime = configurationRepository.findConfigurationUpdateDate(name) ?: databaseUpdateTime
    }

    override fun registerWatcher(scheduler: ScheduledExecutorService) {
        this.watcher = scheduler.scheduleWithFixedDelay({
            configurationRepository.findConfigurationUpdateDate(name)
                ?.takeIf { it.isAfter(databaseUpdateTime) }
                ?.run {
                    journalist.logger.info("Propagation | $displayName has been changed in remote source, updating current instance...")
                    loadRemoteConfiguration()
                }
        }, 10, 10, TimeUnit.SECONDS)
    }

    override fun update(name: String, content: String): Result<Unit, ErrorResponse> =
        when (name) {
            name -> standard.validateAndLoad(content, configuration::class.createInstance(), configuration)
            else -> notFoundError("Unsupported configuration $name")
        }
        .peek { journalist.logger.info("Propagation | $displayName has updated, updating sources...") }
        .map { standard.render(it).orElseThrow(ThrowingFunction.identity()) }
        .map { configurationRepository.saveConfiguration(name, it) }
        .peek { journalist.logger.info("Propagation | Sources have been updated successfully") }
        .map { loadRemoteConfiguration() }

    override fun resolve(name: String): Result<String, ErrorResponse> =
        when (name) {
            this.name -> standard.render(configuration)
                .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, "Cannot load configuration: ${it.message}") }
            else -> errorResponse(BAD_REQUEST, "Unsupported configuration $name")
        }

    override fun shutdown() {
        watcher?.cancel(false)
    }

}
