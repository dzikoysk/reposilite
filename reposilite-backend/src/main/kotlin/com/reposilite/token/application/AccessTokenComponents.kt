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
