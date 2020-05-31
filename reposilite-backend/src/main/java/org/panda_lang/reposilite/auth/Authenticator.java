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
import org.panda_lang.reposilite.utils.Result;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Authenticator {

    private final TokenService tokenService;

    public Authenticator(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Result<Session, String> authDefault(Context context) {
        return authUri(context, context.req.getRequestURI());
    }

    public Result<Session, String> authUri(Context context, String uri) {
        Result<Session, String> authResult = auth(context);

        if (authResult.getError().isDefined()) {
            return authResult;
        }

        Session session = authResult.getValue().get();

        if (!session.hasPermission(uri)) {
            return Result.error("Unauthorized access");
        }

        return authResult;
    }

    public Result<Session, String> auth(Context context) {
        String authorization = context.header("authorization");

        if (authorization == null) {
            return Result.error("Authorization credentials are not specified");
        }

        if (!authorization.startsWith("Basic")) {
            return Result.error("Unsupported auth method");
        }

        String base64Credentials = authorization.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

        String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            return Result.error("Invalid authorization credentials");
        }

        String alias = values[0];
        String rawToken = values[1];

        Token token = tokenService.getToken(alias);

        if (token == null) {
            return Result.error("Invalid authorization credentials");
        }

        boolean authorized = TokenService.B_CRYPT_TOKENS_ENCODER.matches(rawToken, token.getToken());

        if (!authorized) {
            return Result.error("Invalid authorization credentials");
        }

        return Result.ok(new Session(token));
    }

}
