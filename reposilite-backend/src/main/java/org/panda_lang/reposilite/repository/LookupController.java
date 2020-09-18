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
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.frontend.FrontendService;
import org.panda_lang.reposilite.utils.Result;

import java.util.concurrent.ExecutorService;

public final class LookupController implements Handler {

    private final boolean hasProxied;
    private final FrontendService frontend;
    private final LookupService lookupService;
    private final ProxyService proxyService;

    public LookupController(
            Configuration configuration,
            ExecutorService executorService,
            FrontendService frontendService,
            LookupService lookupService,
            RepositoryService repositoryService,
            FailureService failureService) {

        this.frontend = frontendService;
        this.lookupService = lookupService;
        this.hasProxied = configuration.proxied.size() > 0;

        this.proxyService = new ProxyService(
                configuration.storeProxied,
                configuration.rewritePathsEnabled,
                configuration.proxied,
                executorService,
                failureService,
                repositoryService);
    }

    @Override
    public void handle(Context context) {
        Reposilite.getLogger().info("LOOKUP " + context.req.getRequestURI() + " from " + context.req.getRemoteAddr());
        Result<Context, ErrorDto> lookupResponse = lookupService.findLocal(context);

        if (isProxied(lookupResponse)) {
            if (hasProxied) {
                lookupResponse = lookupResponse.orElse(localError -> proxyService
                        .findProxied(context)
                        .map(future -> context.result(future.thenAccept(result -> result
                                .map(context::json)
                                .mapError(error -> ResponseUtils.errorResponse(context, error))))));
            }

            if (isProxied(lookupResponse)) {
                lookupResponse = lookupResponse.mapError(proxiedError -> new ErrorDto(HttpStatus.SC_NOT_FOUND, proxiedError.getMessage()));
            }
        }

        lookupResponse.onError(error -> {
            context.res.setCharacterEncoding("UTF-8");
            context.status(error.getStatus())
                    .contentType("text/html")
                    .result(frontend.forMessage(error.getMessage()));
        });
    }

    private boolean isProxied(Result<Context, ErrorDto> lookupResponse) {
        return lookupResponse.containsError() && lookupResponse.getError().getStatus() == HttpStatus.SC_USE_PROXY;
    }

}
