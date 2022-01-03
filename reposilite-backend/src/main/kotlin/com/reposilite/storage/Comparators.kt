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
package com.reposilite.storage

import java.math.BigInteger
import kotlin.math.max

class FilesComparator<T>(
    cachedValue: (T) -> List<String>,
    private val isDirectory: (T) -> Boolean
) : VersionComparator<T>(cachedValue) {

    override fun compare(version: T, toVersion: T): Int =
        when {
            isDirectory(version) == isDirectory(toVersion) -> super.compare(version, toVersion)
            isDirectory(toVersion) -> 1
            else -> -1
        }

}

open class VersionComparator<T>(
    private val versionMapper: (T) -> List<String>,
) : Comparator<T> {

    companion object {

        private val defaultVersionPattern = Regex("[-._]")

        fun asVersion(value: String): List<String> =
            defaultVersionPattern.split(value)

        fun sortStrings(sequence: Sequence<String>): Sequence<String> =
            sequence
                .map { Pair(it, asVersion(it)) }
                .sortedWith(VersionComparator { cache -> cache.second })
                .map { it.first }

    }

    override fun compare(version: T, toVersion: T): Int =
        compareVersions(versionMapper(version), versionMapper(toVersion))

    private fun compareVersions(version: List<String>, toVersion: List<String>): Int {
        for (index in 0 until max(version.size, toVersion.size)) {
            val fragment = version.getOrElse(index) { "0" }
            val baseIsDigit = fragment.isDigit()
            val toFragment = toVersion.getOrElse(index) { "0" }

            val result =
                // Compare current version to the other
                if (baseIsDigit && toFragment.isDigit()) {
                    try {
                        fragment.toLong().compareTo(toFragment.toLong())
                    } catch (numberFormatException: NumberFormatException) {
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
    isNotEmpty() && !toCharArray().any { !Character.isDigit(it) }
