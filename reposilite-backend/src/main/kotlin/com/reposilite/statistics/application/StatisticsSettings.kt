package com.reposilite.statistics.application

import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.Settings
import com.reposilite.statistics.api.ResolvedRequestsInterval
import com.reposilite.statistics.api.ResolvedRequestsInterval.MONTHLY
import net.dzikoysk.cdn.entity.Contextual

@Contextual
@Doc(title = "Statistics", description = "Statistics module configuration.")
data class StatisticsSettings(
    @Doc(title = "Resolved Requests Interval", description = """
        How often Reposilite should divide recorded requests into separated groups.
        With higher precision you can get more detailed timestamps, but it'll increase database size.
        It's not that important for small repos with low traffic, but public instances should not use daily interval.
    """)
    val resolvedRequestsInterval: ResolvedRequestsInterval = MONTHLY
) : Settings
