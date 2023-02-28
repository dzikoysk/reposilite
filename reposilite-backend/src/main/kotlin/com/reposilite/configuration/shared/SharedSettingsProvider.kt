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

import com.reposilite.configuration.shared.api.SharedSettings
import panda.std.reactive.MutableReference
import panda.std.reactive.mutableReference

typealias SharedSettingsClass = Class<out SharedSettings>
typealias Domains = Map<SharedSettingsClass, MutableReference<SharedSettings>>

data class SharedSettingsProvider(val domains: Domains) {

    companion object {

        fun createStandardProvider(classes: Collection<SharedSettingsClass>): SharedSettingsProvider =
            classes
                .map { it.getConstructor().newInstance() }
                .map { mutableReference(it) }
                .associateBy { it.get()::class.java }
                .let { SharedSettingsProvider(it) }

    }

}
