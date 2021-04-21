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

package org.panda_lang.reposilite.resource;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.panda_lang.reposilite.Reposilite;

public class FrontendHandler implements Handler {

    private final Reposilite reposilite;

    public FrontendHandler(Reposilite reposilite) {
        this.reposilite = reposilite;
    }

    @OpenApi(
        operationId = "getApp",
        summary = "Get frontend application",
        description = "Returns Vue.js application wrapped into one app.js file",
        tags = { "Resource" },
        responses = {
                @OpenApiResponse(status = "200", description = "Default response")
        }
    )
    @Override
    public void handle(Context context) {
        context.result(reposilite.getFrontendService().getApp())
                .header("Content-Type", "application/javascript")
                .res.setCharacterEncoding("UTF-8");
    }
}
