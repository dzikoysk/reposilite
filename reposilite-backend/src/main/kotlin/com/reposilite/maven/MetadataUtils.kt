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

import com.reposilite.shared.FilesUtils.getExtension
import com.reposilite.web.error.ErrorResponse
import panda.std.Result
import panda.utilities.StringUtils
import panda.utilities.text.Joiner
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TreeSet

internal object MetadataUtils {

    private const val ESCAPE_DOT = "`.`"

    private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        .withLocale(Locale.getDefault())
        .withZone(ZoneOffset.UTC)

    @JvmStatic
    fun toSortedBuilds(repository: Repository, directory: Path): Result<Array<Path>, ErrorResponse> {
        val result = repository.getFiles(directory)

        if (result.isOk) {
            val paths: MutableCollection<Path> = TreeSet() // Path comparator may be needed

            for (path in result.get()) {
                paths.add(directory.resolve(path.getName(0)))
            }

            return Result.ok(paths.toTypedArray())
        }

        return result.map { null }
    }

    @JvmStatic
    fun toSortedVersions(repository: Repository, directory: Path): Result<List<Path>, ErrorResponse> =
        repository.getFiles(directory)
            .map { list ->
                list
                .filter { path: Path -> path.parent.endsWith(directory) }
                .filter { file: Path -> repository.isDirectory(file) }
                .sorted()
                .toList()
            }

    internal fun toSortedIdentifiers(artifact: String, version: String, builds: Array<Path>): List<String> =
        builds
            .sorted()
            .map { build -> toIdentifier(artifact, version, build) }
            .filterNot { text -> StringUtils.isEmpty(text) }
            .distinct()
            .toList()

    internal fun isNotChecksum(path: Path, identifier: String): Boolean =
        path.fileName.toString()
            .takeUnless { it.endsWith(".md5") }
            ?.takeUnless { it.endsWith(".sha1") }
            ?.takeIf { it.contains("$identifier.") || it.contains("$identifier-") } != null

    internal fun toBuildFiles(repository: Repository, directory: Path, identifier: String): List<Path> =
        repository.getFiles(directory).get().asSequence()
            .filter { path -> path.parent == directory }
            .filter { path -> isNotChecksum(path, identifier) }
            .filter { file -> repository.exists(file) }
            .sorted()
            .toList()

    @JvmStatic
    fun toIdentifier(artifact: String, version: String, build: Path): String =
        build.fileName.toString()
            .let { it.replace("." + getExtension(it), StringUtils.EMPTY) }
            .replace("$artifact-", StringUtils.EMPTY)
            .replace("$version-", StringUtils.EMPTY)
            .let { declassifyIdentifier(it) }

    private fun declassifyIdentifier(identifier: String): String {
        val occurrences = StringUtils.countOccurrences(identifier, "-")

        // no action required
        if (occurrences == 0) {
            return identifier
        }
        val occurrence = identifier.indexOf("-")

        // process identifiers without classifier or build number
        if (occurrences == 1) {
            return if (isBuildNumber(identifier.substring(occurrence + 1))) identifier else identifier.substring(0, occurrence)
        }

        // remove classifier
        return if (isBuildNumber(identifier.substring(occurrence + 1, identifier.indexOf("-", occurrence + 1)))) identifier.substring(
            0,
            identifier.indexOf("-", occurrence + 1)
        ) else identifier.substring(0, occurrence)
    }

    @Throws(IOException::class)
    internal fun toUpdateTime(repository: Repository, file: Path): String {
        val result = repository.getLastModifiedTime(file)

        return if (result.isOk) {
            TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(result.get().toMillis()))
        } else {
            throw IOException(result.error.message)
        }
    }

    fun toGroup(elements: Array<String>): String =
        Joiner.on(".")
            .join(elements.copyOfRange(0, elements.size)) { value: String -> if (value.contains(".")) value.replace(".", ESCAPE_DOT) else value }
            .toString()

    internal fun toGroup(elements: Array<String>, toShrink: Int): String =
        toGroup(shrinkGroup(elements, toShrink)).replace(ESCAPE_DOT, ".")

    private fun shrinkGroup(elements: Array<String>, toShrink: Int): Array<String> =
        elements.copyOfRange(0, elements.size - toShrink)

    fun toGroup(metadataFilePath: Path): String {
        val builder = StringBuilder()
        val iterator: Iterator<Path> = metadataFilePath.parent.parent.iterator()

        while (iterator.hasNext()) {
            val path = iterator.next()
            builder.append(path.fileName.toString())

            if (iterator.hasNext()) {
                builder.append('.')
            }
        }

        return builder.toString()
    }

    private fun isBuildNumber(content: String): Boolean =
        content
            .any { !Character.isDigit(it) }
            .not()

}