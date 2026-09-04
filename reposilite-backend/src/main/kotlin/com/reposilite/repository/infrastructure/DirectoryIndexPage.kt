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

package com.reposilite.repository.infrastructure

import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.FileType

internal fun createDirectoryIndexPage(basePath: String, uri: String, visibleFiles: List<FileDetails>): String {
    val formattedUri = basePath + uri.removePrefix("/")
    val parentUri = formattedUri.removeSuffix("/").substringBeforeLast("/").ifEmpty { "/" }

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
                        <a href='$parentUri'>Parent Directory</a>
                    </li>
                    ${visibleFiles.flatMap {
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
