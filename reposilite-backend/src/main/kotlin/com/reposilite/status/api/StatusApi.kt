package com.reposilite.status.api

data class InstanceStatusResponse(
    val version: String,
    val uptime: String,
    val usedMemory: String,
    val usedThreads: Int,
    val failuresCount: Int
)
