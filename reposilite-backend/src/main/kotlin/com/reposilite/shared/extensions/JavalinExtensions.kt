package com.reposilite.shared.extensions

import com.reposilite.web.http.acceptsBody
import com.reposilite.web.http.contentDisposition
import com.reposilite.web.http.contentLength
import com.reposilite.web.silentClose
import io.javalin.http.ContentType
import io.javalin.http.Context
import java.io.InputStream

internal fun Context.resultAttachment(
    name: String,
    contentType: ContentType,
    contentLength: Long,
    compressionStrategy: String,
    data: InputStream
): Context {
    if (!contentType.isHumanReadable) {
        contentDisposition(""""attachment; filename="$name" """)
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
    return this
}