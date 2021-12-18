package com.reposilite.plugin.api

import com.reposilite.VERSION

annotation class Plugin(
    val name: String,
    val version: String = VERSION,
    val dependencies: Array<String> = []
)

interface Facade