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
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.error.ResponseUtils;

public final class AuthEndpoint implements RepositoryController {

    private final AuthService authService;

    public AuthEndpoint(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Context handleContext(Context ctx) {
        return ResponseUtils.response(ctx, authService.authByHeader(ctx.headerMap()));
    }

}
