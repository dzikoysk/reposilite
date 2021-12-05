package com.reposilite.shared.extensions

import com.reposilite.web.http.acceptsBody
import com.reposilite.web.http.contentDisposition
import com.reposilite.web.http.contentLength
import com.reposilite.web.silentClose
import io.javalin.http.ContentType
import io.javalin.http.Context
import java.io.InputStream

internal fun Context.resultAttachment(name: String, contentType: ContentType, contentLength: Long, data: InputStream): Context {
    contentType(contentType)

    if (contentLength > 0) {
        contentLength(contentLength)
    }

    if (!contentType.isHumanReadable) {
        contentDisposition(""""attachment; filename="$name" """)
    }

    if (acceptsBody()) {
        result(data)
    } else {
        data.silentClose()
    }

    /*
    data.use {
        if (acceptsBody()) {
            it.copyTo(output())
        }
    }
     */

    return this
}