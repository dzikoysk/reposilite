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
package org.panda_lang.reposilite.shared

object MimeTypes {

    const val MIME_PLAIN = "text/plain"
    const val MIME_HTML = "text/html"
    const val MIME_XML = "text/xml"
    const val MIME_OCTET_STREAM = "application/octet-stream"
    const val MIME_JAVASCRIPT = "application/javascript"
    const val MIME_JSON = "application/json"

    private val STANDARD_TYPES: MutableMap<String, String> = HashMap()

    init {
        // Fallback list of basic mime types used by Maven repository
        // ~ https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types

        /* Text */

        STANDARD_TYPES["txt"] = MIME_PLAIN
        STANDARD_TYPES["css"] = "text/css"
        STANDARD_TYPES["csv"] = "text/csv"
        STANDARD_TYPES["htm"] = MIME_HTML
        STANDARD_TYPES["html"] = MIME_HTML
        STANDARD_TYPES["xml"] = MIME_PLAIN

        /* Image */

        STANDARD_TYPES["ico"] = "image/vnd.microsoft.icon"
        STANDARD_TYPES["jpeg"] = "image/jpeg"
        STANDARD_TYPES["jpg"] = "image/jpg"
        STANDARD_TYPES["png"] = "image/png"
        STANDARD_TYPES["tif"] = "image/tiff"
        STANDARD_TYPES["tiff"] = "image/tiff"

        /* Font */

        STANDARD_TYPES["otf"] = "font/otf"
        STANDARD_TYPES["ttf"] = "font/ttf"

        /* Application */

        STANDARD_TYPES["bin"] = MIME_OCTET_STREAM
        STANDARD_TYPES["bz"] = "application/x-bzip"
        STANDARD_TYPES["bz2"] = "application/x-bzip2"
        STANDARD_TYPES["cdn"] = "application/cdn"
        STANDARD_TYPES["gz"] = "application/gzip"
        STANDARD_TYPES["js"] = MIME_JAVASCRIPT
        STANDARD_TYPES["json"] = MIME_JSON
        STANDARD_TYPES["mpkg"] = "application/vnd.apple.installer+xml"
        STANDARD_TYPES["jar"] = "application/java-archive"
        STANDARD_TYPES["rar"] = "application/vnd.rar"
        STANDARD_TYPES["sh"] = "application/x-sh"
        STANDARD_TYPES["tar"] = "application/x-tar"
        STANDARD_TYPES["xhtml"] = "application/xhtml+xml"
        STANDARD_TYPES["yaml"] = "application/yaml"
        STANDARD_TYPES["zip"] = "application/zip"
        STANDARD_TYPES["7z"] = "application/x-7z-compressed"
    }

    fun getMimeType(name: String, defaultTemplate: String) =
        STANDARD_TYPES.getOrDefault(name, defaultTemplate)

}