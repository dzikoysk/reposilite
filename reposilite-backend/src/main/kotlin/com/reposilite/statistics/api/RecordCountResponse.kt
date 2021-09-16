package com.reposilite.statistics.api

data class RecordCountResponse(
    val count: Long,
    val records: List<Record>
)