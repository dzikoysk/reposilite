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

package com.reposilite.javadocs.application

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import io.javalin.openapi.JsonSchema

@JsonSchema(requireNonNulls = false)
@Doc(title = "Javadoc", description = "Javadoc settings")
data class JavadocSettings(
    @get:Doc(title = "Enabled", description = "Enable javadoc integration")
    val enabled: Boolean = true,
    @get:Doc(title = "Suffixes", description = "Artifact file suffixes served through the javadoc viewer, e.g. -javadoc.jar, -groovydoc.jar, -docs.zip.")
    val suffixes: List<String> = listOf("-javadoc.jar", "-groovydoc.jar"),
) : SharedSettings
