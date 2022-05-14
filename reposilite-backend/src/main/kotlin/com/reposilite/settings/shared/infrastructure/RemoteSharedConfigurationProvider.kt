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

package com.reposilite.settings.shared.infrastructure

import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.settings.ConfigurationFacade
import com.reposilite.settings.ConfigurationProvider
import com.reposilite.settings.shared.SharedConfigurationFacade
import com.reposilite.settings.shared.SharedSettings
import com.reposilite.settings.shared.SharedSettingsReference
import com.reposilite.status.FailureFacade
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import panda.std.Result
import panda.std.asError
import panda.std.asSuccess
import panda.std.ok
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class RemoteSharedConfigurationProvider(
    private val journalist: Journalist,
    private val failureFacade: FailureFacade,
    private val configurationFacade: ConfigurationFacade,
    private val sharedConfigurationFacade: SharedConfigurationFacade
) : ConfigurationProvider, Journalist {

    override val name = "shared-configuration"
    override val displayName = "Shared (remote) configuration"

    private var databaseUpdateTime = Instant.ofEpochMilli(0)
    private var watcher: ScheduledFuture<*>? = null

    override fun initialize(): Boolean {
        journalist.logger.info("Loading ${displayName.lowercase()} from database")
        return loadRemoteConfiguration()
    }

    private fun loadRemoteConfiguration(): Boolean =
        configurationFacade.findConfiguration(name)
            ?.let { updateSharedSettings(it) }
            ?.also { refreshUpdateTime() }
            ?.isOk
            ?: generateConfiguration().isOk

    private fun generateConfiguration(): Result<Unit, *> =
        renderConfiguration()
            .let { configurationFacade.saveConfiguration(name, it) }
            .also { refreshUpdateTime() }
            .asSuccess<Unit, Nothing>()

    private fun renderConfiguration(): String =
        sharedConfigurationFacade.names()
            .associateWith { sharedConfigurationFacade.getSettingsReference<SharedSettings>(it)!!.get() }
            .let { DEFAULT_OBJECT_MAPPER.writeValueAsString(it) }

    private fun refreshUpdateTime() {
        this.databaseUpdateTime = configurationFacade.findConfigurationUpdateDate(name) ?: databaseUpdateTime
    }

    private fun updateSharedSettings(content: String): Result<Unit, Collection<Pair<SharedSettingsReference<*>, Exception>>> {
        val updateResult = sharedConfigurationFacade.updateSharedSettings(content)

        updateResult
            .filter { (_, result) -> result.isOk }
            .forEach { (ref) -> journalist.logger.info("$displayName | Domain '${ref.name}' has been loaded from database") }

        val failures = updateResult.filter { (_, result) -> result.isErr }

        failures.forEach { (ref, result) ->
            journalist.logger.error("$displayName | Cannot update '${ref.name}' due to ${result.error}")
            journalist.logger.debug("$displayName | Source:")
            journalist.logger.debug(content)
            failureFacade.throwException(displayName, result.error)
        }

        return if (failures.isNotEmpty())
            failures
                .map { (ref, result) -> ref to result.error }
                .asError()
        else ok()
    }

    override fun update(content: String): Result<Unit, ErrorResponse> =
        updateSharedSettings(content)
            .also {
                journalist.logger.info("Propagation | $displayName has updated, updating sources...")
                configurationFacade.saveConfiguration(name, renderConfiguration())
                journalist.logger.info("Propagation | Sources have been updated successfully")
            }
            .let { it.mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, it.joinToString(",")) } }

    override fun registerWatcher(scheduler: ScheduledExecutorService) {
        this.watcher = scheduler.scheduleWithFixedDelay({
            configurationFacade.findConfigurationUpdateDate(name)
                ?.takeIf { it.isAfter(databaseUpdateTime) }
                ?.run {
                    journalist.logger.info("Propagation | $displayName has been changed in remote source, updating current instance...")
                    loadRemoteConfiguration()
                }
        }, 10, 10, TimeUnit.SECONDS)
    }

    override fun shutdown() {
        watcher?.cancel(false)
    }

    override fun getLogger(): Logger =
        journalist.logger

}
