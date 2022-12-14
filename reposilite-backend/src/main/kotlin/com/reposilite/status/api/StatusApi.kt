package com.reposilite.status.api

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
