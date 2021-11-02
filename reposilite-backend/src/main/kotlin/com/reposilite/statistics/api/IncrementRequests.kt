package com.reposilite.statistics.api

data class IncrementDeployedRequest(
    private val identifier: String,
    private val by: String
)

data class IncrementResolvedRequest(
    val identifier: String
)
