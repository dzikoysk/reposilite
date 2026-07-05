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

package com.reposilite.frontend

import panda.std.letIf

internal object BasePathFormatter {

    private val pathRegex = Regex("^/|/$")

    fun formatBasePath(originBasePath: String): String =
        originBasePath
            .letIf({ it.isNotEmpty() && !it.startsWith("/") }, { "/$it" })
            .letIf({ it.isNotEmpty() && !it.endsWith("/")}, { "$it/" })

    fun formatAsViteBasePath(path: String): String =
        path
            .takeIf { hasCustomBasePath(it) }
            ?.replace(pathRegex, "") // remove first & last slash
            ?: "." // no custom base path

    private fun hasCustomBasePath(path: String): Boolean =
        path != "" && path != "/"

}