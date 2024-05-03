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

package com.reposilite.storage.api

import com.reposilite.storage.getExtension
import java.nio.file.Path
import java.nio.file.Paths
import panda.std.Result
import panda.std.asSuccess
import panda.std.letIf

/**
 * [Path] alternative, represents location of resource in [com.reposilite.storage.StorageProvider]
 */
class Location private constructor(private val uri: String) {

    companion object {

        private val empty = Location("")
        private val multipleSlashes = Regex("/+")
        private val multipleDirectoryOperators = Regex("\\.{2,}")

        @JvmStatic
        fun of(uri: String): Location {
            return uri
                .replaceBefore(":", "")
                .replace(":", "")
                .replace(multipleDirectoryOperators, ".")
                .replace("\\", "/")
                .replace(multipleSlashes, "/")
                .letIf({ it.startsWith("/") }) { it.removePrefix("/") }
                .letIf({ it.endsWith("/") }) { it.removeSuffix("/") }
                .let { Location(it) }
        }

        @JvmStatic
        fun of(path: Path): Location =
            of(path.normalize().toString())

        @JvmStatic
        fun of(root: Path, path: Path): Location =
            of(root.relativize(path.normalize()))

        @JvmStatic
        fun empty(): Location =
            empty

    }

    fun toPath(): Result<Path, String> {
        if (uri.contains(":") || uri.contains("\\") || uri.contains(multipleDirectoryOperators)) {
            return Result.error("Illegal path operator in URI")
        }
        return Paths.get(uri).normalize().asSuccess()
    }

    fun resolve(subLocation: String): Location =
        resolve(subLocation.toLocation())

    fun resolve(subLocation: Location): Location =
        "$uri/${subLocation.uri}".toLocation()

    fun resolveSibling(sibling: String): Location =
        getParent().resolve(sibling)

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
        when (uri.contains("/")) {
            true -> uri.substringBeforeLast("/").toLocation()
            else -> empty()
        }

    fun getExtension(): String =
        getSimpleName().getExtension()

    fun getSimpleName(): String =
        uri.substringAfterLast("/")

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            javaClass != other?.javaClass -> false
            else -> uri == (other as Location).uri
        }

    override fun hashCode(): Int =
        uri.hashCode()

    override fun toString(): String =
        uri

}

fun String?.toLocation(): Location =
    when {
        this != null -> Location.of(this)
        else -> Location.empty()
    }
