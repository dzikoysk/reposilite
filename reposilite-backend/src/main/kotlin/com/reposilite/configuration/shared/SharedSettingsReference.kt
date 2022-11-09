package com.reposilite.configuration.shared

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import panda.std.Result
import panda.std.Result.supplyThrowing
import java.io.InputStream
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

interface SharedSettingsReference<S : SharedSettings> {

    val type: KClass<out S>
    val name: String
    val schema: Supplier<InputStream>

    fun get(): S
    fun update(value: S): Result<S, out Exception>

}

internal class DefaultSharedSettingsReference<T : SharedSettings>(
    override val type: KClass<out T>,
    override val schema: Supplier<InputStream>,
    private val getter: () -> T,
    private val setter: (T) -> Unit
) : SharedSettingsReference<T> {

    override val name = type.findAnnotation<Doc>()!!.title.sanitizeURLParam()

    override fun get(): T =
        getter()

    override fun update(value: T): Result<T, out Exception> =
        supplyThrowing { setter(value) }
            .mapErr { IllegalStateException("Cannot update settings reference $name", it) }
            .map { get() }

    private fun String.sanitizeURLParam(): String =
        lowercase().replace(' ', '_')

}
