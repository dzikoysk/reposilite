package com.reposilite.plugin.api

import com.reposilite.VERSION
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS)
@Retention(RUNTIME)
annotation class Plugin(
    val name: String,
    val version: String = VERSION,
    val dependencies: Array<String> = []
)

interface Facade