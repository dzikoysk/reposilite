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

package com.reposilite.frontend.application

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import io.javalin.openapi.JsonSchema

@JsonSchema(requireNonNulls = false)
@Doc(title = "Frontend", description = "Frontend settings")
data class FrontendSettings(
    @get:Doc(title = "Id", description = "Repository id used in Maven repository configuration")
    val id: String = "reposilite-repository",
    @get:Doc(title = "Title", description = "The title displayed on the frontend homepage.")
    val title: String = "Reposilite Repository",
    @get:Doc(title = "Description", description = "The description displayed on the frontend homepage.")
    val description: String = "Public Maven repository hosted through the Reposilite",
    @get:Doc(title = "Organisation Website", description = "Link to organization's website.")
    val organizationWebsite: String = "https://reposilite.com",
    @get:Doc(title = "Organisation Logo", description = "Link to organization's logo.")
    val organizationLogo: String = "https://avatars.githubusercontent.com/u/88636591",
    @get:Doc(title = "ICP License", description = """
        Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.
        In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.
    """)
    val icpLicense: String = "",
) : SharedSettings
