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

package com.reposilite.storage.api

import com.reposilite.storage.getExtension
import panda.std.Result
import panda.std.asSuccess
import panda.std.letIf
import java.nio.file.Path
import java.nio.file.Paths

/**
 * [Path] alternative, represents location of resource in [com.reposilite.storage.StorageProvider]
 */
@Suppress("DataClassPrivateConstructor")
data class Location private constructor(private val uri: String) {

    companion object {

        private val multipleSlashes = Regex("/+")

        @JvmStatic
        fun of(uri: String): Location =
            uri.replace("\\", "/")
                .replace(multipleSlashes, "/")
                .letIf({ it.startsWith("/") }) { it.removePrefix("/") }
                .letIf({ it.endsWith("/") }) { it.removeSuffix("/") }
                .let { Location(it) }

        @JvmStatic
        fun of(path: Path): Location =
            path.toString().toLocation()

        @JvmStatic
        fun of(root: Path, path: Path): Location =
            of(root.relativize(path))

        @JvmStatic
        fun empty(): Location =
            "".toLocation()

    }

    fun toPath(): Result<Path, String> {
        if (uri.contains("..") || uri.contains(":") || uri.contains("\\")) {
            return Result.error("Illegal path operator in URI")
        }

        return Paths.get(uri).normalize().asSuccess()
    }

    fun resolve(subLocation: String): Location =
        resolve(subLocation.toLocation())

    fun resolve(subLocation: Location): Location =
        "$uri/${subLocation.uri}".toLocation()

    fun resolveSibling(sibling: String): Location =
        uri.substringBeforeLast("/")
            .toLocation()
            .resolve(sibling)

    fun replace(element: String, replacement: String): Location =
        uri.replace(element, replacement).toLocation()

    fun contains(value: String): Boolean =
        uri.contains(value)

    fun endsWith(suffix: String): Boolean =
        uri.endsWith(suffix)

    fun locationBeforeLast(delimiter: String, defaultValue: String? = null): Location =
        uri.substringBeforeLast(delimiter, defaultValue ?: uri).toLocation()

    fun locationAfterLast(delimiter: String, defaultValue: String? = null): Location =
        uri.substringAfterLast(delimiter, defaultValue ?: uri).toLocation()

    fun getParent(): Location =
        uri.substringBeforeLast("/").toLocation()

    fun getExtension(): String =
        getSimpleName().getExtension()

    fun getSimpleName(): String =
        uri.substringAfterLast("/")

    override fun toString(): String =
        uri

}

fun String?.toLocation(): Location =
    if (this != null)
        Location.of(this)
    else
        Location.empty()
