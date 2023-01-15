package com.reposilite.javadocs.page

import com.reposilite.javadocs.api.JavadocResponse
import com.reposilite.shared.ErrorResponse
import panda.std.Result
import panda.std.asSuccess
import panda.utilities.StringUtils

internal class JavadocPlainEmptyFilePage(private val javadocPlainFile: JavadocPlainFile) : JavadocPage {

    override fun render(): Result<JavadocResponse, ErrorResponse> {
        return JavadocResponse(javadocPlainFile.contentType.mimeType, StringUtils.EMPTY).asSuccess()
    }

}