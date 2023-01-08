/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.token.application

import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenRepository
import com.reposilite.token.ExportService
import com.reposilite.token.infrastructure.InMemoryAccessTokenRepository
import com.reposilite.token.infrastructure.SqlAccessTokenRepository
import org.jetbrains.exposed.sql.Database

class AccessTokenComponents(
    private val journalist: Journalist,
    private val database: Database?
) : PluginComponents {

    private fun temporaryRepository(): AccessTokenRepository =
        InMemoryAccessTokenRepository()

    private fun persistentRepository(): AccessTokenRepository =
        when (database) {
            null -> InMemoryAccessTokenRepository()
            else -> SqlAccessTokenRepository(database)
        }

    private fun exportService(): ExportService =
        ExportService()

    fun accessTokenFacade(
        temporaryRepository: AccessTokenRepository = temporaryRepository(),
        persistentRepository: AccessTokenRepository = persistentRepository(),
        exportService: ExportService = exportService()
    ): AccessTokenFacade =
        AccessTokenFacade(
            journalist = journalist,
            temporaryRepository = temporaryRepository,
            persistentRepository = persistentRepository,
            exportService = exportService
        )

}
