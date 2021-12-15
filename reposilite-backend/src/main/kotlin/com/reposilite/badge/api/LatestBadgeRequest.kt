package com.reposilite.badge.api

class LatestBadgeRequest(
    val repository: String,
    val gav: String,
    val name: String? = null,
    val color: String? = null,
    val prefix: String? = null,
    val filter: String? = null
)