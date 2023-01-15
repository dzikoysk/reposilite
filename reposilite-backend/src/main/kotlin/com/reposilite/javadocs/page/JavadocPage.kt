package com.reposilite.javadocs.page

import com.reposilite.javadocs.api.JavadocResponse
import com.reposilite.shared.ErrorResponse
import panda.std.Result

internal interface JavadocPage {

    fun render(): Result<JavadocResponse, ErrorResponse>

}