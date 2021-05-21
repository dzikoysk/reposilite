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

package org.panda_lang.reposilite.maven.repository.infrastructure;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.*;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteContextFactory;
import org.panda_lang.reposilite.failure.api.ErrorResponse;
import org.panda_lang.reposilite.failure.FailureService;
import org.panda_lang.reposilite.maven.repository.LookupResponse;
import org.panda_lang.reposilite.maven.repository.LookupService;
import org.panda_lang.reposilite.maven.repository.ProxyService;
import org.panda_lang.reposilite.resource.ResourceFacade;
import org.panda_lang.reposilite.shared.utils.OutputUtils;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;

final class LookupController implements Handler {

    private final boolean hasProxied;
    private final ReposiliteContextFactory contextFactory;
    private final ResourceFacade frontend;
    private final LookupService lookupService;
    private final ProxyService proxyService;
    private final FailureService failureService;

    public LookupController(
            boolean hasProxied,
            ReposiliteContextFactory contextFactory,
            ResourceFacade resourceFacade,
            LookupService lookupService,
            ProxyService proxyService,
            FailureService failureService) {

        this.hasProxied = hasProxied;
        this.contextFactory = contextFactory;
        this.frontend = resourceFacade;
        this.lookupService = lookupService;
        this.proxyService = proxyService;
        this.failureService = failureService;
    }

    @OpenApi(
            operationId = "repositoryLookup",
            summary = "Browse the contents of repositories",
            description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
            tags = { "Repository" },
            pathParams = {
                    @OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true),
                    @OpenApiParam(
                            name = "*/latest",
                            description = "[Optional] Artifact path qualifier with /latest at the end returns latest version of artifact as text/plain"
                    )
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Input stream of requested file", content = {
                            @OpenApiContent(type = ContentType.FORM_DATA_MULTIPART)
                    }),
                    @OpenApiResponse(
                            status = "404",
                            description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository"
                    ),
            }
    )
    @Override
    public void handle(@NotNull Context ctx) {
        ReposiliteContext context = contextFactory.create(ctx);
        context.getLogger().debug("LOOKUP " + context.uri() + " from " + context.address());

        Result<LookupResponse, ErrorResponse> response;

        if (lookupService.exists(context)) {
            try {
                response = lookupService.find(context);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else if (this.hasProxied) {
            response = proxyService.findProxied(context);
        } else {
            response = Result.error(new ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found"));
        }

        handleResult(ctx, context, response);
    }

    private void handleResult(Context ctx, ReposiliteContext context, Result<LookupResponse, ErrorResponse> result) {
        result
            .peek(response -> handleResult(ctx, context, response))
            .onError(error -> handleError(ctx, error));
    }

    private void handleResult(Context ctx, ReposiliteContext context, LookupResponse response) {
        response.getFileDetails().peek(details -> {
            if (details.getContentLength() > 0) {
                ctx.res.setContentLengthLong(details.getContentLength());
            }

            if (response.isAttachment()) {
                ctx.res.setHeader("Content-Disposition", "attachment; filename=\"" + details.getName() + "\"");
            }
        });

        response.getContentType().peek(ctx.res::setContentType);
        response.getValue().peek(ctx::result);

        context.result().peek(result -> {
            try {
                if (OutputUtils.isProbablyOpen(ctx.res.getOutputStream())) {
                    result.accept(ctx.res.getOutputStream());
                }
            } catch (IOException exception) {
                failureService.throwException(context.uri(), exception);
            }
        });
    }

    private void handleError(Context ctx, ErrorResponse error) {
        ctx.result(frontend.forMessage(error.getMessage()))
                .status(error.getStatus())
                .contentType("text/html")
                .res.setCharacterEncoding("UTF-8");
    }
}
