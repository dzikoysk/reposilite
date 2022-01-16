package com.reposilite.maven.api

import com.reposilite.storage.api.Location

class LatestBadgeRequest(
    val repository: String,
    val gav: Location,
    val name: String? = null,
    val color: String? = null,
    val prefix: String? = null,
    val filter: String? = null
)