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

package com.reposilite.web.application

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import io.javalin.openapi.JsonSchema

@JsonSchema(requireNonNulls = false)
@Doc(title = "Web", description = "General web settings")
data class WebSettings(
    @get:Doc(title = "Forwarded IP", description = """
        Any kind of proxy services change real ip.
        The origin ip should be available in one of the headers. <br />
        Nginx: X-Forwarded-For <br />
        Cloudflare: CF-Connecting-IP <br />
        Popular: X-Real-IP
    """)
    val forwardedIp: String = "X-Forwarded-For",
) : SharedSettings
