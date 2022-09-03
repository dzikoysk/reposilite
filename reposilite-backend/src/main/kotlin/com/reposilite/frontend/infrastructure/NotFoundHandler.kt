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

package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.NotFoundResponse

internal class NotFoundHandler(private val frontendFacade: FrontendFacade) : Handler, ExceptionHandler<NotFoundResponse> {

    // It does not support async handlers
    private val defaultNotFoundHandler: (Context) -> Unit = { ctx ->
        if (ctx.resultInputStream() == null) {
            ctx.status(NOT_FOUND).html(frontendFacade.createNotFoundPage(ctx.req().requestURI, ""))
        }
    }

    override fun handle(ctx: Context) =
        defaultNotFoundHandler(ctx)

    override fun handle(exception: NotFoundResponse, ctx: Context) =
        defaultNotFoundHandler(ctx)

}
