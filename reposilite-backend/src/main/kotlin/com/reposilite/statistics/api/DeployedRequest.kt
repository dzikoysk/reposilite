package com.reposilite.statistics.api

import java.time.LocalDateTime

data class DeployedRequest(
    private val identifier: Identifier,
    private val date: LocalDateTime,
    private val by: String
)