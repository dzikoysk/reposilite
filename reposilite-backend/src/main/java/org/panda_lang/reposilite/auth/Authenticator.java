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
import org.jetbrains.annotations.Nullable;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.api.ErrorDto;
import org.panda_lang.reposilite.api.ErrorUtils;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.reposilite.ReposiliteUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class Authenticator {

    private final Configuration configuration;
    private final RepositoryService repositoryService;
    private final TokenService tokenService;

    public Authenticator(Configuration configuration, RepositoryService repositoryService, TokenService tokenService) {
        this.configuration = configuration;
        this.repositoryService = repositoryService;
        this.tokenService = tokenService;
    }

    public Result<Pair<String[], Repository>, ErrorDto> authDefaultRepository(Context context, String uri) {
        return authRepository(context, ReposiliteUtils.normalizeUri(configuration, repositoryService, uri));
    }

    public Result<Pair<String[], Repository>, ErrorDto> authRepository(Context context, String uri) {
        String[] path = StringUtils.split(uri, "/");
        String repositoryName = path[0];

        if (StringUtils.isEmpty(repositoryName)) {
            return ErrorUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Unsupported request");
        }

        Repository repository = repositoryService.getRepository(repositoryName);

        if (repository == null) {
            return ErrorUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Repository " + repositoryName  + " not found");
        }

        // auth hidden repositories
        if (repository.isHidden()) {
            Result<Session, String> authResult = authDefault(context);

            if (authResult.containsError()) {
                return ErrorUtils.error(HttpStatus.SC_UNAUTHORIZED, "Unauthorized request");
            }
        }

        return Result.ok(new Pair<>(path, repository));
    }

    public Result<Session, String> authDefault(Context context) {
        return authUri(context, context.req.getRequestURI());
    }

    public Result<Session, String> authUri(Context context, String uri) {
        return authUri(context.headerMap(), uri);
    }

    public Result<Session, String> authUri(Map<String, String> header, String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }

        Result<Session, String> authResult = auth(header);

        if (authResult.containsError()) {
            return authResult;
        }

        Session session = authResult.getValue();

        if (!session.hasPermission(uri)) {
            return Result.error("Unauthorized access attempt");
        }

        Reposilite.getLogger().info("AUTH " + session.getToken().getAlias() + " accessed " + uri);
        return authResult;
    }

    public Result<Session, String> auth(Map<String, String> header) {
        String authorization = header.get("Authorization");

        if (authorization == null) {
            return Result.error("Authorization credentials are not specified");
        }

        if (!authorization.startsWith("Basic")) {
            return Result.error("Unsupported auth method");
        }

        String base64Credentials = authorization.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

        return auth(credentials);
    }

    public Result<Session, String> auth(@Nullable String credentials) {
        if (credentials == null) {
            return Result.error("Authorization credentials are not specified");
        }

        String[] values = StringUtils.split(credentials, ":");

        if (values.length != 2) {
            return Result.error("Invalid authorization credentials");
        }

        Token token = tokenService.getToken(values[0]);

        if (token == null) {
            return Result.error("Invalid authorization credentials");
        }

        boolean authorized = TokenService.B_CRYPT_TOKENS_ENCODER.matches(values[1], token.getToken());

        if (!authorized) {
            return Result.error("Invalid authorization credentials");
        }

        boolean manager = configuration.managers.contains(token.getAlias());
        return Result.ok(new Session(repositoryService, token, manager));
    }

}
