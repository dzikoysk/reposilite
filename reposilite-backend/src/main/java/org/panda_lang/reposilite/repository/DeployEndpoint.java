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

package org.panda_lang.reposilite.repository;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.ContentType;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.jetbrains.annotations.NotNull;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteContextFactory;
import org.panda_lang.reposilite.error.ResponseUtils;

public final class DeployEndpoint implements Handler {

    private final ReposiliteContextFactory contextFactory;
    private final DeployService deployService;

    public DeployEndpoint(ReposiliteContextFactory contextFactory, DeployService deployService) {
        this.contextFactory = contextFactory;
        this.deployService = deployService;
    }

    @OpenApi(
            operationId = "repositoryDeploy",
            summary = "Deploy artifact to the repository",
            description = "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
            tags = { "Repository" },
            pathParams = {
                    @OpenApiParam(name = "*", description = "Artifact path qualifier", required = true)
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Input stream of requested file", content = {
                            @OpenApiContent(type = ContentType.FORM_DATA_MULTIPART)
                    }),
                    @OpenApiResponse(
                            status = "401",
                            description = "Returns 401 for invalid credentials"
                    ),
                    @OpenApiResponse(
                            status = "405",
                            description = "Returns 405 if deployment is disabled in configuration"
                    ),
                    @OpenApiResponse(
                            status = "500",
                            description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file"
                    ),
                    @OpenApiResponse(
                            status = "507",
                            description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file"
                    ),
            }
    )
    @Override
    public void handle(@NotNull Context ctx) {
        ReposiliteContext context = contextFactory.create(ctx);
        context.getLogger().info("DEPLOY " + context.uri() + " from " + context.address());

        deployService.deploy(context)
                    .map(ctx::json)
                        .onError(error -> context.getLogger().debug("Cannot deploy artifact due to: " + error.getMessage()))
                        .mapErr(error -> ResponseUtils.errorResponse(ctx, error));
    }

}
