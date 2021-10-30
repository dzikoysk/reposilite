package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFoundError
import io.javalin.http.ContentType.APPLICATION_CDN
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.ContentType.APPLICATION_YAML
import io.javalin.http.HttpCode.BAD_REQUEST
import net.dzikoysk.cdn.Cdn
import net.dzikoysk.cdn.CdnFactory
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.Result.ok
import panda.std.asSuccess

class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val sharedConfigurationService: SharedConfigurationService
) {

    fun resolveConfiguration(name: String): Result<SettingsResponse, ErrorResponse> =
        when (name) {
            "shared.configuration.cdn" -> SettingsResponse(APPLICATION_CDN, CdnFactory.createStandard().render(getSharedConfiguration())).asSuccess()
            "shared.configuration.json" -> SettingsResponse(APPLICATION_JSON, CdnFactory.createJsonLike().render(getSharedConfiguration())).asSuccess()
            "shared.configuration.yaml" -> SettingsResponse(APPLICATION_YAML, CdnFactory.createYamlLike().render(getSharedConfiguration())).asSuccess()
            else -> notFoundError("Unsupported configuration $name")
        }

    fun updateConfiguration(request: SettingsUpdateRequest): Result<Unit, ErrorResponse> =
        when (request.name) {
            "shared.configuration.cdn" -> CdnFactory.createStandard().validateAndLoad(request.content, SharedConfiguration(), getSharedConfiguration())
            "shared.configuration.json" -> CdnFactory.createJsonLike().validateAndLoad(request.content, SharedConfiguration(), getSharedConfiguration())
            "shared.configuration.yaml" -> CdnFactory.createYamlLike().validateAndLoad(request.content, SharedConfiguration(), getSharedConfiguration())
            else -> notFoundError("Unsupported configuration ${request.name}")
        }

    private fun Cdn.validateAndLoad(source: String, testConfiguration: Any, configuration: Any): Result<Unit, ErrorResponse> =
        try {
            load(Source.of(source), testConfiguration) // validate
            load(Source.of(source), configuration)
            ok(Unit)
        } catch (exception: Exception) {
            errorResponse(BAD_REQUEST, "Cannot load configuration: ${exception.message}")
        }

    fun verifySharedConfiguration() =
        sharedConfigurationService.verifySharedConfiguration()

    fun loadSharedConfiguration(): SharedConfiguration =
        sharedConfigurationService.loadSharedConfiguration()

    fun getSharedConfiguration(): SharedConfiguration =
        sharedConfigurationService.sharedConfiguration

}