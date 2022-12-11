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
