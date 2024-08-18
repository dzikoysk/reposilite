package com.reposilite.maven.infrastructure

import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.FileType

internal fun createDirectoryIndexPage(basePath: String, uri: String, directory: DirectoryInfo): String {
    val formattedUri = basePath + uri.removePrefix("/")

    // language=html
    return """
        <!DOCTYPE html>
        <html lang='en'>
            <head>
                <title>Index of $formattedUri</title>
                <meta charset='utf-8'>
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
                        <a href='${formattedUri.substringBeforeLast("/")}'>Parent Directory</a>
                    </li>
                    ${directory.files.flatMap { 
                        listOf(
                            "<li class='${it.type.name.lowercase()}'>",
                            "<a href='$formattedUri/${it.name}'>${it.name}${if (it.type == FileType.DIRECTORY) "/" else ""}</a>",
                            "</li>"
                        )
                    }.joinToString(separator = "")}
                </ul>
            </body>
        </html>
    """.trimIndent()
}