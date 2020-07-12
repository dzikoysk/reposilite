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
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.api.ErrorUtils;
import org.panda_lang.reposilite.config.Configuration;

import java.util.List;

public final class AuthApiController implements RepositoryController {

    private final Configuration configuration;
    private final Authenticator authenticator;

    public AuthApiController(Configuration configuration, Authenticator authenticator) {
        this.configuration = configuration;
        this.authenticator = authenticator;
    }

    @Override
    public Context handleContext(Context ctx) {
        return authenticator
                .auth(ctx.headerMap())
                .map(session -> {
                    Token token = session.getToken();

                    List<String> repositories = token.hasMultiaccess()
                            ? configuration.getRepositories()
                            : session.getRepositories();

                    return new AuthDto(
                            configuration.getManagers().contains(token.getAlias()),
                            token.getPath(),
                            repositories
                    );
                })
                .map(ctx::json)
                .orElseGet(error -> ErrorUtils.error(ctx, HttpStatus.SC_UNAUTHORIZED, error));
    }

}
