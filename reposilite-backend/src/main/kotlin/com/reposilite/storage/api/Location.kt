/*
 * Copyright (c) 2020-2026 dzikoysk
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

import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequest
import com.reposilite.shared.badRequestError
import com.reposilite.storage.getExtension
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.nio.file.Paths
import panda.std.Result
import panda.std.asSuccess

/**
 * [Path] alternative, represents location of resource in [com.reposilite.storage.StorageProvider]
 */
class Location private constructor(private val uri: String) {

    companion object {

        private val empty = Location("")
        private val multipleSlashes = Regex("/+")
        private val pathOperator = Regex("\\.{2,}")
        private val illegalCharacters = Regex("[:<>\"'\\\\|?*]")

        @JvmStatic
        fun of(uri: String): Location {
            if (illegalCharacters.containsMatchIn(uri) || pathOperator.containsMatchIn(uri) || uri.any { it.isISOControl() }) {
                throw UnsupportedLocationException(uri)
            }
            return Location(uri.replace(multipleSlashes, "/").trim('/'))
        }

        @JvmStatic
        fun ofRequest(uri: String): Result<Location, ErrorResponse> =
            parse(uri).filter(Location::isCanonicalResourcePath) { badRequest("Resource path has to be canonical") }

        internal fun parse(uri: String): Result<Location, ErrorResponse> =
            try {
                of(uri).asSuccess()
            } catch (exception: UnsupportedLocationException) {
                badRequestError(exception.message)
            }

        @JvmStatic
        fun of(path: Path): Location =
            of(path.normalize().toString().replace("\\", "/"))

        @JvmStatic
        fun of(root: Path, path: Path): Location =
            of(root.relativize(path.normalize()))

        @JvmStatic
        fun empty(): Location =
            empty

    }

    internal fun isCanonicalResourcePath(): Boolean =
        uri.split('/').none { it == "." }

    internal fun toUrlPath(): String =
        uri.split('/').joinToString("/", transform = ::encodePathSegment)

    internal fun resolveChild(name: String): Result<Location, ErrorResponse> =
        when {
            name.isEmpty() || '/' in name -> badRequestError("Expected one non-empty path segment")
            else -> ofRequest(name).map { resolve(it) }
        }

    fun toPath(): Path =
        Paths.get(uri).normalize()

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

internal fun encodePathSegment(name: String): String =
    URLEncoder.encode(name, UTF_8).replace("+", "%20")

fun String?.toLocation(): Location =
    when {
        this != null -> Location.of(this)
        else -> Location.empty()
    }
