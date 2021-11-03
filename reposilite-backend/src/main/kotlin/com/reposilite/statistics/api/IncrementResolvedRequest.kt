package com.reposilite.statistics.api

data class IncrementResolvedRequest(
    val identifier: Identifier,
    val count: Long = 1
)