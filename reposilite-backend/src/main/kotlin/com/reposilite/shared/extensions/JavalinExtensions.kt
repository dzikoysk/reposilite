/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.shared.extensions

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.reposilite.web.http.acceptsBody
import com.reposilite.web.http.contentDisposition
import com.reposilite.web.http.contentLength
import com.reposilite.web.silentClose
import io.javalin.http.ContentType
import io.javalin.http.Context
import java.io.InputStream
import java.net.URLEncoder

internal fun Context.resultAttachment(
    name: String,
    contentType: ContentType,
    contentLength: Long,
    compressionStrategy: String,
    data: InputStream
) {
    if (!contentType.isHumanReadable) {
        contentDisposition("""attachment; filename="$name"; filename*=utf-8''${URLEncoder.encode(name, "utf-8")}""")
    }

    if (compressionStrategy == "none" && contentLength > 0) {
        contentLength(contentLength) // Using this with GZIP ends up with "Premature end of Content-Length delimited message body".
    }

    if (acceptsBody()) {
        result(data)
    } else {
        data.silentClose()
    }

    contentType(contentType)
}

internal class ContentTypeSerializer : StdSerializer<ContentType> {

    constructor() : this(null)

    constructor(type: Class<ContentType>?) : super(type)

    override fun serialize(value: ContentType, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.mimeType)
    }

}
