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

package org.panda_lang.nanomaven.auth;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import org.panda_lang.nanomaven.utils.Result;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Authenticator {

    private TokenService tokenService;

    public Authenticator(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Result<Session, Response> auth(IHTTPSession session) {
        String authorization = session.getHeaders().get("authorization");

        if (authorization == null) {
            return error("Authorization credentials are not specified");
        }

        if (!authorization.startsWith("Basic")) {
            return error("Unsupported auth method");
        }

        String base64Credentials = authorization.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

        String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            return error("Invalid authorization credentials");
        }

        String alias = values[0];
        String rawToken = values[1];

        Token token = tokenService.getToken(alias);

        if (token == null) {
            return error("Invalid authorization credentials");
        }

        boolean authorized = TokenService.B_CRYPT_TOKENS_ENCODER.matches(rawToken, token.getToken());

        if (!authorized) {
            return error("Invalid authorization credentials");
        }

        if (!session.getUri().startsWith(token.getPath())) {
            return error("Invalid authorization credentials");
        }

        return Result.ok(new Session(token));
    }

    private Result<Session, Response> error(String message) {
        return Result.error(NanoHTTPD.newFixedLengthResponse(Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, message));
    }

}
