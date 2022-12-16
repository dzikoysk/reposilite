package com.reposilite.status.api

import java.time.Instant

data class InstanceStatusResponse(
    val version: String,
    val latestVersion: String,
    val uptime: Long,
    val usedMemory: Double,
    val maxMemory: Int,
    val usedThreads: Int,
    val maxThreads: Int,
    val failuresCount: Int
)

data class StatusSnapshot(
    val at: Instant = Instant.now(),
    val memory: Int,
    val threads: Int
)