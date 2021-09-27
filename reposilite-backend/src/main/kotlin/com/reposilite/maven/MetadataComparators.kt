/*
 * Copyright (c) 2021 dzikoysk
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
package com.reposilite.maven

import java.math.BigInteger
import java.util.function.Function
import java.util.function.Predicate
import java.util.regex.Pattern
import kotlin.math.max

internal class FilesComparator<T>(
    cachedValue: Function<T, Array<String>>,
    private val isDirectory: Predicate<T>
) : VersionComparator<T>(cachedValue) {

    override fun compare(version: T, toVersion: T): Int {
        if (isDirectory.test(version) != isDirectory.test(toVersion)) {
            return if(isDirectory.test(toVersion)) 1 else -1
        }

        return super.compare(version, toVersion)
    }

}

internal open class VersionComparator<T>(
    private val versionMapper: Function<T, Array<String>>,
) : Comparator<T> {

    companion object {

        private val DEFAULT_VERSION_PATTERN = Pattern.compile("[-._]")

        fun asVersion(value: String): Array<String> =
            DEFAULT_VERSION_PATTERN.split(value)

        fun sortStrings(collection: Collection<String>): List<String> =
            sortWithCache(collection, Function.identity())

        private fun <T> sortWithCache(collection: Collection<T>, nameMapper: Function<T, String>): List<T> =
            collection
                .map { Pair(it, asVersion(nameMapper.apply(it))) }
                .sortedWith(VersionComparator { cache -> cache.second })
                .map { it.first }

    }

    override fun compare(version: T, toVersion: T): Int =
        compareVersions(versionMapper.apply(version), versionMapper.apply(toVersion))

    private fun compareVersions(version: Array<String>, toVersion: Array<String>): Int {
        for (index in 0 until max(version.size, toVersion.size)) {
            val fragment = version.getOrElse(index) { "0" }
            val baseIsDigit = fragment.isDigit()
            val toFragment = toVersion.getOrElse(index) { "0" }

            val result =
                // Compare current version to the other
                if (baseIsDigit && toFragment.isDigit()) {
                    try {
                        fragment.toLong().compareTo(toFragment.toLong())
                    }
                    catch (numberFormatException: NumberFormatException) {
                        BigInteger(fragment).compareTo(BigInteger(toFragment))
                    }
                }
                // Prioritize digits over strings
                else if (baseIsDigit || toFragment.isDigit()) {
                    if (baseIsDigit) -1 else 1
                }
                // Compare strings
                else {
                    fragment.compareTo(toFragment)
                }

            if (result != 0) {
                return result
            }
        }

        return version.size.compareTo(toVersion.size)
    }

}

private fun String.isDigit(): Boolean =
    isNotEmpty() && !this.toCharArray().any { !Character.isDigit(it) }
