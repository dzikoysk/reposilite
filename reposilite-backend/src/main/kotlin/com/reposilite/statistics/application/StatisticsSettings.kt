package com.reposilite.statistics.application

import java.io.Serializable

/**
 * Statistics module configuration.
 */
data class StatisticsSettings(
    /*@Doc(title = "Resolved Requests Interval",
        summary = "How often Reposilite should divide recorded requests into separated groups.",
        description = """
            How often Reposilite should divide recorded requests into separated groups.
            With higher precision you can get more detailed timestamps, but it'll increase database size.
            It's not that important for small repos with low traffic, but public instances should not use daily interval.
            """)*/
    val resolvedRequestsInterval: ResolvedRequestsInterval = ResolvedRequestsInterval.MONTHLY
) : Serializable {
    enum class ResolvedRequestsInterval : Serializable {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}
