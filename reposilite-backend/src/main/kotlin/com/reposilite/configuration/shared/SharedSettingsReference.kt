/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
