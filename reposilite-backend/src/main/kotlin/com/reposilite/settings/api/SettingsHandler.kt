package com.reposilite.settings.api

import java.util.function.Consumer
import java.util.function.Supplier

interface SettingsHandler<T> {
    val name: String
    val type: Class<T>
    fun get(): T
    fun update(value: T)

    companion object {
        fun <T> of(name: String, type: Class<T>, getter: Supplier<T>, updater: Consumer<T>): SettingsHandler<T> = object: SettingsHandler<T> {
            override val name = name
            override val type = type
            override fun get(): T = getter.get()
            override fun update(value: T): Unit = updater.accept(value)
        }
    }
}
