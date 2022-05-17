package com.reposilite.configuration.shared

import panda.std.reactive.MutableReference
import panda.std.reactive.mutableReference
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

data class SharedSettingsProvider(
    val domains: Map<KClass<out SharedSettings>, MutableReference<SharedSettings>>
) {

    companion object {

        fun createStandardProvider(classes: Collection<KClass<out SharedSettings>>): SharedSettingsProvider =
            classes
            .map { it.createInstance() }
            .map { mutableReference(it) }
            .associateBy { it.get()::class }
            .let { SharedSettingsProvider(it) }
    }

}