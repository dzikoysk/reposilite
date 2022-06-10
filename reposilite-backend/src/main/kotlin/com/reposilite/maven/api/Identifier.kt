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

package com.reposilite.maven.api

import java.util.UUID

const val REPOSITORY_NAME_MAX_LENGTH = 32
const val GAV_MAX_LENGTH = 1024

/**
 * Gav is a string that represents path in associated repository.
 * The name is a short for Group:Artifact:Version, but it real-world usage it represents all entries within the specified repository.
 */
data class Identifier(val repository: String, val gav: String) {

    init {
        if (repository.length > REPOSITORY_NAME_MAX_LENGTH) {
            throw IllegalStateException("Repository name cannot exceed $GAV_MAX_LENGTH characters")
        }

        if (gav.length > GAV_MAX_LENGTH) {
            throw IllegalStateException("Gav cannot exceed $GAV_MAX_LENGTH characters")
        }
    }

    fun toUUID(): UUID =
        UUID.nameUUIDFromBytes(toString().encodeToByteArray())

    override fun toString(): String =
        "/$repository/$gav"

}
