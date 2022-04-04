/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.settings.api

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.maven.application.RepositoriesSettings
import com.reposilite.settings.EnumResolver
import com.reposilite.settings.SettingsModule
import com.reposilite.settings.SubtypeResolver
import com.reposilite.settings.createStandardSchemaGenerator
import com.reposilite.statistics.application.StatisticsSettings
import com.reposilite.storage.StorageProviderFactory
import com.reposilite.storage.application.StorageProviderSettings
import com.reposilite.web.application.WebSettings
import panda.std.reactive.MutableReference
import panda.std.reactive.mutableReference
import java.util.ServiceLoader

const val SHARED_CONFIGURATION_FILE = "configuration.shared.json"

class SharedConfiguration {

    val domains = mutableMapOf(
        AuthenticationSettings::class.java to mutableReference(AuthenticationSettings()),
        WebSettings::class.java to mutableReference(WebSettings()),
        RepositoriesSettings::class.java to mutableReference(RepositoriesSettings()),
        FrontendSettings::class.java to mutableReference(FrontendSettings()),
        StatisticsSettings::class.java to mutableReference(StatisticsSettings())
    )

    @Suppress("UNCHECKED_CAST")
    fun <T> forDomain(type: Class<T>): MutableReference<T> =
        domains[type] as MutableReference<T>

    inline fun <reified T> forDomain(): MutableReference<T> =
        forDomain(T::class.java)

}

fun createSharedConfigurationSchemaGenerator(): SchemaGenerator {
    val storageProviders = ServiceLoader.load(StorageProviderFactory::class.java)
        .associate { it.settingsType to it.type }

    val storageEnumResolver = EnumResolver {
        if (it.name == "type")
            storageProviders[it.declaringType.erasedType]?.let { type -> listOf(type) }
        else null
    }
    val storageSubtypeResolver = SubtypeResolver { declaredType, context ->
        if (declaredType.erasedType == StorageProviderSettings::class.java)
            storageProviders.keys.toList().map { clazz -> context.typeContext.resolveSubtype(declaredType, clazz) }
        else null
    }

    return createStandardSchemaGenerator(
        SettingsModule(
            subtypeResolvers =  listOf(storageSubtypeResolver),
            enumResolvers = listOf(storageEnumResolver)
        )
    )
}