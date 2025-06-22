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

package com.reposilite.javadocs

import com.reposilite.storage.api.FileDetails
import org.intellij.lang.annotations.Language

internal object JavadocView {

    /**
     * Creates a new index.html file as a "holder" for the actual javadoc, so in the future we can have custom things embedded, like
     * switching between documents easily, downloading documents etc.
     * WARNING/NOTE: this html contains an iframe which points to a docindex.html, that must be in the same directory as the index.html!
     */
    fun index(unpackedIndexPath: String, javadocList: List<FileDetails>): String {
        val dropdownOptions = javadocList.joinToString("\n") {
            val version = it.name
            val label = "Version $version"
            """<option value="$version">$label</option>"""
        }

        @Suppress("CssUnresolvedCustomProperty", "HtmlUnknownTarget")
        @Language("html")
        val source = """
        <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <title>Reposilite - JavaDoc</title>
            </head>
            <style>
                :root {
                    --nav-height: 3rem;
                }
                body {
                    height: calc(100vh - 170px);
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    font-family: Arial, Helvetica, sans-serif;
                } 
                .sticky-nav {
                    position: fixed;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    top: 0;
                    left: 0;
                    width: calc(100vw - 2rem);
                    height: var(--nav-height);
                    padding-left: 1rem;
                    padding-right: 1rem;
                    background-color: #325064;
                    color: #FFFFFF;
                }
                .doc {
                    border-top: solid 3px #588DB0; 
                    position: fixed;
                    top: var(--nav-height);
                    left: 0;
                    width: 100%;
                    height: calc(100vh - var(--nav-height));
                }
                .row {
                    display: flex;
                    justify-content: flex-start;
                    align-items: center;
                }
                a {
                    text-decoration: none;
                    color: white;
                    width: auto;
                    margin-right: 2rem;
                }
                .title {
                    margin-right: 2rem;
                }
                a:hover {
                    color: #e2dfdf;
                }
                select {
                    background-color: #325064;
                    color: white;
                    border: 1px solid #ccc;
                    padding: 0.4rem;
                    font-size: 1rem;
                    margin-left: 1rem;
                }                
            </style>
            <body>
                <div class="sticky-nav">
                    <div class="row">
                        <a class="title" href="/"><h3>Reposilite</h3></a>
                        <a id='raw'><h4>Raw docs</h4></a>
                        <select id="javadoc-selector">
                            $dropdownOptions
                        </select>
                        <!--<a href="#p"><h5>Download JavaDoc</h5></a> todo-->
                    </div>
                </div>
                <iframe id="javadoc" class="doc" src="$unpackedIndexPath" sandbox="allow-scripts"></iframe>
                <script>
                    if (!window.location.href.endsWith("/")) {
                        document.getElementById("javadoc").src = window.location.href + '$unpackedIndexPath';
                    }
                    document.getElementById('raw').href = window.location.href + '/raw/index.html';
                    
                    const pathParts = window.location.pathname.split('/');
                    const currentVersion = pathParts[pathParts.length - 1];
                    const options = document.getElementById('javadoc-selector').options;
                    for (let i = 0; i < options.length; i++) {
                        options[i].removeAttribute('selected');
                        if (options[i].value === currentVersion) {
                            options[i].setAttribute('selected', 'selected');
                            break;
                        }
                    }
                    
                    document.getElementById('javadoc-selector').addEventListener('change', function() {
                        const version = this.value;
                        const parts = window.location.href.split('/');
                        parts.pop(); // Remove current version
                        window.location.href = parts.join('/') + '/' + version;
                    });
                </script>
            </body>
        </html>
        """.trimIndent()

        return source
    }

}
