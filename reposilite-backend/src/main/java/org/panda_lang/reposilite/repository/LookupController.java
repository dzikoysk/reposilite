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
import org.panda_lang.reposilite.frontend.FrontendService;
import org.panda_lang.reposilite.utils.Result;

public final class LookupController implements Handler {

    private final FrontendService frontend;
    private final LookupService lookupService;

    public LookupController(FrontendService frontend, LookupService lookupService) {
        this.frontend = frontend;
        this.lookupService = lookupService;
    }

    @Override
    public void handle(Context context) {
        Reposilite.getLogger().info("LOOKUP " + context.req.getRequestURI() + " from " + context.req.getRemoteAddr());

        Result<Context, String> lookupResponse = lookupService
                .serveLocal(context)
                .orElse(localError -> lookupService.serveProxied(context)
                        .map(context::result)
                        .orElse(proxiedError -> Result.error(localError)));

        lookupResponse.onError(error -> {
            Reposilite.getLogger().debug("error=" + error + "; uri=" + context.req.getRequestURI());

            context.res.setCharacterEncoding("UTF-8");
            context.status(HttpStatus.SC_NOT_FOUND)
                    .contentType("text/html")
                    .result(frontend.forMessage(error));
        });
    }

}
