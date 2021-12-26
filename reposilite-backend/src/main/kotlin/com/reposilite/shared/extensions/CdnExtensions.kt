package com.reposilite.shared.extensions

import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import net.dzikoysk.cdn.Cdn
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.asError
import panda.std.asSuccess

fun String.createCdnByExtension(): Result<Cdn, Exception> =
    when {
        endsWith(".cdn") -> KCdnFactory.createStandard().asSuccess()
        endsWith(".yml") || endsWith(".yaml") -> KCdnFactory.createYamlLike().asSuccess()
        endsWith(".json") -> KCdnFactory.createJsonLike().asSuccess()
        else -> UnsupportedOperationException("Unknown format: $this").asError()
    }

fun <T : Any> Cdn.validateAndLoad(source: String, testConfiguration: T, configuration: T): Result<T, ErrorResponse> =
    load(Source.of(source), testConfiguration) // validate
        .flatMap { load(Source.of(source), configuration) }
        .mapErr { ErrorResponse(BAD_REQUEST, "Cannot load configuration: ${it.message}") }