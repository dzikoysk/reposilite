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

package com.reposilite.statistics.application

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.statistics.api.ResolvedRequestsInterval
import com.reposilite.statistics.api.ResolvedRequestsInterval.MONTHLY
import io.javalin.openapi.JsonSchema

@JsonSchema(requireNonNulls = false)
@Doc(title = "Statistics", description = "Statistics module configuration.")
data class StatisticsSettings(
    @get:Doc(title = "Enable statistics", description = """
        In a long run statistics can allocate quite a lot of space in database.
        It's especially a thing for public repositories with a large number of different artifacts (like e.g. mirrored maven-central).
        If you don't really care about statistics used in dashboard/badges, you can just simply disable it.
    """)
    val enabled: Boolean = true,
    @get:Doc(title = "Resolved Requests Interval", description = """
        How often Reposilite should divide recorded requests into separated groups.
        With higher precision you can get more detailed timestamps, but it'll increase database size.
        It's not that important for small repos with low traffic, but public instances should not use daily interval.
    """)
    val resolvedRequestsInterval: ResolvedRequestsInterval = MONTHLY
) : SharedSettings
