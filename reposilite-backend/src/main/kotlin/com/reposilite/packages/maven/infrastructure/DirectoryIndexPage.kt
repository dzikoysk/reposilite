package com.reposilite.packages.maven.infrastructure

import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.FileType

internal fun createDirectoryIndexPage(basePath: String, uri: String, authenticatedFiles: List<FileDetails>): String {
    val formattedUri = basePath + uri.removePrefix("/")

    // language=html
    return """
        <!DOCTYPE html>
        <html lang='en'>
            <head>
                <title>Index of $formattedUri</title>
                <meta charset='utf-8'>
                <base href='${formattedUri.removeSuffix("/")}/'>
                <style>
                li {
                    padding: 2px 10px;
                }
                .back::marker {
                    content: '🔙';
                }
                .directory::marker {
                    content: '📁';
                }
                .file::marker {
                    content: '📄';
                }
                </style>
            </head>
            <body>
                <h1>Index of $formattedUri</h1>
                <ul>
                    <li class='back'>
                        <a href='${formattedUri.removeSuffix("/").substringBeforeLast("/")}'>Parent Directory</a>
                    </li>
                    ${authenticatedFiles.flatMap {
                        val fileSeparator = if (it.type == FileType.DIRECTORY) "/" else ""
        
                        listOf(
                            """<li class="${it.type.name.lowercase()}">""",
                            """<a href="./${it.name}$fileSeparator">${it.name}$fileSeparator</a>""",
                            """</li>"""
                        )
                    }.joinToString(separator = "")}
                </ul>
            </body>
        </html>
    """.trimIndent()
}