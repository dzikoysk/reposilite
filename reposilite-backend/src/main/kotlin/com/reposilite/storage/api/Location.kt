package com.reposilite.storage.api

import com.reposilite.shared.extensions.letIf
import com.reposilite.storage.getExtension
import panda.std.Result
import panda.std.asSuccess
import java.nio.file.Path
import java.nio.file.Paths

/**
 * [Path] alternative, represents location of resource in [StorageProvider]
 */
class Location private constructor(private val uri: String) {

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

    fun locationBeforeLast(delimiter: String, defaultValue: String? = null): Location =
        uri.substringBeforeLast(delimiter, defaultValue ?: uri).toLocation()

    fun locationAfterLast(delimiter: String, defaultValue: String? = null): Location =
        uri.substringAfterLast(delimiter, defaultValue ?: uri).toLocation()

    fun getRootName(): String =
        uri.substringBefore("/")

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