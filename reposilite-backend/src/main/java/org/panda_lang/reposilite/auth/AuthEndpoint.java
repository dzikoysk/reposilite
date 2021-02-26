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

package org.panda_lang.reposilite.auth;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;

public final class AuthEndpoint implements Handler {

    private final AuthService authService;

    public AuthEndpoint(AuthService authService) {
        this.authService = authService;
    }

    @OpenApi(
            operationId = "auth",
            summary = "Get token details",
            description = "Returns details about the requested token",
            tags = { "Auth" },
            headers = {
                    @OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true)
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Details about the token for succeeded authentication", content = {
                            @OpenApiContent(from = AuthDto.class)
                    }),
                    @OpenApiResponse(status = "401", description = "Error message related to the unauthorized access in case of any failure", content = {
                            @OpenApiContent(from = ErrorDto.class)
                    })
            }
    )
    @Override
    public void handle(Context ctx) {
        ResponseUtils.response(ctx, authService.authByHeader(ctx.headerMap()));
    }

}