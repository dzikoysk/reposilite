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
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.ReposiliteUtils;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.utils.ErrorDto;
import org.panda_lang.reposilite.utils.ResponseUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;

public final class RepositoryAuthenticator {

    private final Configuration configuration;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;

    public RepositoryAuthenticator(Configuration configuration, Authenticator authenticator, RepositoryService repositoryService) {
        this.configuration = configuration;
        this.authenticator = authenticator;
        this.repositoryService = repositoryService;
    }

    public Result<Pair<String[], Repository>, ErrorDto> authDefaultRepository(Context context, String uri) {
        return authRepository(context, ReposiliteUtils.normalizeUri(configuration, repositoryService, uri));
    }

    public Result<Pair<String[], Repository>, ErrorDto> authRepository(Context context, String uri) {
        String[] path = StringUtils.split(uri, "/");
        String repositoryName = path[0];

        if (StringUtils.isEmpty(repositoryName)) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Unsupported request");
        }

        Repository repository = repositoryService.getRepository(repositoryName);

        if (repository == null) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Repository " + repositoryName  + " not found");
        }

        // auth hidden repositories
        if (repository.isHidden()) {
            Result<Session, String> authResult = authenticator.authByUri(context.headerMap(), context.req.getRequestURI());

            if (authResult.containsError()) {
                return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, "Unauthorized request");
            }
        }

        return Result.ok(new Pair<>(path, repository));
    }

}
