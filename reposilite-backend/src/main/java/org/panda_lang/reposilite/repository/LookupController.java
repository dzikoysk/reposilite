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
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteContextFactory;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.frontend.FrontendService;
import org.panda_lang.reposilite.utils.OutputUtils;
import org.panda_lang.reposilite.utils.Result;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class LookupController implements Handler {

    private final boolean hasProxied;
    private final ReposiliteContextFactory contextFactory;
    private final FrontendService frontend;
    private final LookupService lookupService;
    private final ProxyService proxyService;
    private final FailureService failureService;

    public LookupController(
            boolean hasProxied,
            ReposiliteContextFactory contextFactory,
            FrontendService frontendService,
            LookupService lookupService,
            ProxyService proxyService,
            FailureService failureService) {

        this.hasProxied = hasProxied;
        this.contextFactory = contextFactory;
        this.frontend = frontendService;
        this.lookupService = lookupService;
        this.proxyService = proxyService;
        this.failureService = failureService;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        ReposiliteContext context = contextFactory.create(ctx);
        Reposilite.getLogger().info("LOOKUP " + context.uri() + " from " + context.address());

        Result<LookupResponse, ErrorDto> response;

        try {
            response = lookupService.findLocal(context);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (isProxied(response)) {
            if (hasProxied) {
                handle(ctx, context, proxyService.findProxied(context));
                return;
            }

            if (isProxied(response)) {
                response = response.mapError(proxiedError -> new ErrorDto(HttpStatus.SC_NOT_FOUND, proxiedError.getMessage()));
            }
        }

        handleResult(ctx, context, response);
    }

    private void handle(Context ctx, ReposiliteContext context, Result<CompletableFuture<Result<LookupResponse, ErrorDto>>, ErrorDto> response) {
        response
            .map(task -> task.thenAccept(result -> handleResult(ctx, context, result)))
            .peek(ctx::result)
            .onError(error -> handleError(ctx, error));
    }

    private void handleResult(Context ctx, ReposiliteContext context, Result<LookupResponse, ErrorDto> result) {
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

    private void handleError(Context ctx, ErrorDto error) {
        ctx.result(frontend.forMessage(error.getMessage()))
                .status(error.getStatus())
                .contentType("text/html")
                .res.setCharacterEncoding("UTF-8");
    }

    private boolean isProxied(Result<?, ErrorDto> lookupResponse) {
        return lookupResponse.containsError() && lookupResponse.getError().getStatus() == HttpStatus.SC_USE_PROXY;
    }

}
