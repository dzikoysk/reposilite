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

package com.reposilite.shared.extensions

import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.badRequest
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
        .mapErr { badRequest("Cannot load configuration: ${it.message}") }
