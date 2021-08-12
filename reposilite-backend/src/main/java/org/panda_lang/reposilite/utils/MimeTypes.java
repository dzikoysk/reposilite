/*
 * Copyright (c) 2020 Dzikoysk
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

package org.panda_lang.reposilite.utils;

import java.util.HashMap;
import java.util.Map;

public final class MimeTypes {

    private static final Map<String, String> STANDARD_TYPES = new HashMap<>();

    // Fallback list of basic mime types used by Maven repository
    // ~ https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
    static {
        /* Text */

        STANDARD_TYPES.put("txt", "text/plain");
        STANDARD_TYPES.put("css", "text/css");
        STANDARD_TYPES.put("csv", "text/csv");
        STANDARD_TYPES.put("htm", "text/html");
        STANDARD_TYPES.put("html", "text/html");
        STANDARD_TYPES.put("xml", "text/xml");

        /* Image */

        STANDARD_TYPES.put("ico", "image/vnd.microsoft.icon");
        STANDARD_TYPES.put("jpeg", "image/jpeg");
        STANDARD_TYPES.put("jpg", "image/jpg");
        STANDARD_TYPES.put("png", "image/png");
        STANDARD_TYPES.put("tif", "image/tiff");
        STANDARD_TYPES.put("tiff", "image/tiff");

        /* Font */

        STANDARD_TYPES.put("otf", "font/otf");
        STANDARD_TYPES.put("ttf", "font/ttf");

        /* Application */

        STANDARD_TYPES.put("bin", "application/application/octet-stream");
        STANDARD_TYPES.put("bz", "application/x-bzip");
        STANDARD_TYPES.put("bz2", "application/x-bzip2");
        STANDARD_TYPES.put("gz", "application/gzip");
        STANDARD_TYPES.put("js", "application/javascript");
        STANDARD_TYPES.put("json", "application/json");
        STANDARD_TYPES.put("mpkg", "application/vnd.apple.installer+xml");
        STANDARD_TYPES.put("jar", "application/java-archive");
        STANDARD_TYPES.put("rar", "application/vnd.rar");
        STANDARD_TYPES.put("sh", "application/x-sh");
        STANDARD_TYPES.put("tar", "application/x-tar");
        STANDARD_TYPES.put("xhtml", "application/xhtml+xml");
        STANDARD_TYPES.put("zip", "application/zip");
        STANDARD_TYPES.put("7z", "application/x-7z-compressed");
    }

    private MimeTypes() {}

    public static String getMimeType(String name, String defaultTemplate) {
        return STANDARD_TYPES.getOrDefault(name, defaultTemplate);
    }

}
