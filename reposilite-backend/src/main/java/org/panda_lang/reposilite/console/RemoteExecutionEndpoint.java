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

package org.panda_lang.reposilite.console;

import io.javalin.http.Context;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteContextFactory;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Result;

import java.util.List;

public final class RemoteExecutionEndpoint implements RepositoryController {

    private static final int MAX_COMMAND_LENGTH = 1024;

    private final Authenticator authenticator;
    private final ReposiliteContextFactory contextFactory;
    private final Console console;

    public RemoteExecutionEndpoint(Authenticator authenticator, ReposiliteContextFactory contextFactory, Console console) {
        this.authenticator = authenticator;
        this.contextFactory = contextFactory;
        this.console = console;
    }

    @Override
    public Context handleContext(Context ctx) {
        ReposiliteContext context = contextFactory.create(ctx);
        Reposilite.getLogger().info("REMOTE EXECUTION " + context.uri() + " from " + context.address());

        Result<Session, String> authResult = authenticator.authByHeader(context.headers());

        if (authResult.isErr()) {
            return ResponseUtils.errorResponse(ctx, HttpStatus.SC_UNAUTHORIZED, authResult.getError());
        }

        Session session = authResult.get();

        if (!session.isManager()) {
            return ResponseUtils.errorResponse(ctx, HttpStatus.SC_UNAUTHORIZED, "Authenticated user is not a manger");
        }

        String command = ctx.body();

        if (StringUtils.isEmpty(command)) {
            return ResponseUtils.errorResponse(ctx, HttpStatus.SC_BAD_REQUEST, "Missing command");
        }

        if (command.length() > MAX_COMMAND_LENGTH) {
            return ResponseUtils.errorResponse(ctx, HttpStatus.SC_BAD_REQUEST, "The given command exceeds allowed length (" + command.length() + " > " + MAX_COMMAND_LENGTH + ")");
        }

        Reposilite.getLogger().info(session.getAlias() + " (" + context.address() + ") requested command: " + command);
        Result<List<String>, List<String>> result = console.execute(command);

        return ctx.json(new RemoteExecutionDto(result.isOk(), result.isOk() ? result.get() : result.getError()));
    }

}
