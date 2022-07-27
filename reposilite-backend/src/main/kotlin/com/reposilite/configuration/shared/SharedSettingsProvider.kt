package com.reposilite.configuration.shared

import com.reposilite.configuration.shared.api.SharedSettings
import panda.std.reactive.MutableReference
import panda.std.reactive.mutableReference
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

typealias SharedSettingsClass = KClass<out SharedSettings>
typealias Domains = Map<SharedSettingsClass, MutableReference<SharedSettings>>

data class SharedSettingsProvider(val domains: Domains) {

    companion object {

        fun createStandardProvider(classes: Collection<SharedSettingsClass>): SharedSettingsProvider =
            classes
                .map { it.createInstance() }
                .map { mutableReference(it) }
                .associateBy { it.get()::class }
                .let { SharedSettingsProvider(it) }

    }

}
