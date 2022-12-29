package com.reposilite.frontend

import panda.std.letIf

internal object BasePathFormatter {

    private val pathRegex = Regex("^/|/$")

    fun formatBasePath(originBasePath: String): String =
        originBasePath
            .letIf({ it.isNotEmpty() && !it.startsWith("/") }, { "/$it" })
            .letIf({ it.isNotEmpty() && !it.endsWith("/")}, { "$it/" })

    fun formatAsViteBasePath(path: String): String =
        path
            .takeIf { hasCustomBasePath(it) }
            ?.replace(pathRegex, "") // remove first & last slash
            ?: "." // no custom base path

    private fun hasCustomBasePath(path: String): Boolean =
        path != "" && path != "/"

}