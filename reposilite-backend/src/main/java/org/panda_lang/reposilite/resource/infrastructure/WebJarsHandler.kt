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
package org.panda_lang.reposilite.resource.infrastructure

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.apache.http.HttpStatus
import org.eclipse.jetty.util.resource.Resource
import org.panda_lang.reposilite.shared.utils.FilesUtils.getMimeType

internal class WebJarsHandler : Handler {

    @OpenApi(
        operationId = "getWebJars",
        summary = "Get web libraries in jars",
        description = "Currently available libraries: Swagger UI",
        tags = ["Resource"],
        responses = [OpenApiResponse(status = "200", description = "Returns input stream of matched resource"), OpenApiResponse(
            status = "404",
            description = "Returns 404 if the requested resource is not available in the current classpath"
        )]
    )
    override fun handle(context: Context) {
        val resource = Resource.newClassPathResource("META-INF/resources" + context.path())

        if (resource == null) {
            context.status(HttpStatus.SC_NOT_FOUND)
            return
        }

        with(context.result(resource.inputStream)) {
            contentType(getMimeType(context.path(), "text/plain"))
            res.characterEncoding = "UTF-8"
        }
    }

}