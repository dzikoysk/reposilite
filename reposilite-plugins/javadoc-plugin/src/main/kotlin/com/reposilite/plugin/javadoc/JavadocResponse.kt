package com.reposilite.plugin.javadoc


class JavadocResponse(contentType: String, response: Any?) {

    val contentType: String
    val response: Any?

    init {
        this.contentType = contentType
        this.response = response
    }
}