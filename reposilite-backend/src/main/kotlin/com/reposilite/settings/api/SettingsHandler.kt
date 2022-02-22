package com.reposilite.settings.api

import java.util.function.Supplier
import java.util.function.UnaryOperator

interface SettingsHandler<T> {
    val name: String
    val type: Class<T>
    fun get(): T
    fun update(value: T): T

    companion object {
        fun <T> of(name: String, type: Class<T>, getter: Supplier<T>, updater: UnaryOperator<T>): SettingsHandler<T> {
            return object : SettingsHandler<T> {
                override val name = name
                override val type = type
                override fun get(): T = getter.get()
                override fun update(value: T): T = updater.apply(value)
            }
        }
    }
}
