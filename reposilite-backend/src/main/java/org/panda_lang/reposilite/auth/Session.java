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

import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.utilities.commons.StringUtils;

import java.util.Collection;
import java.util.Collections;

public final class Session {

    private final RepositoryService repositoryService;
    private final Token token;
    private final boolean manager;

    public Session(RepositoryService repositoryService, Token token, boolean manager) {
        this.repositoryService = repositoryService;
        this.token = token;
        this.manager = manager;
    }

    public boolean isManager() {
        return manager;
    }

    public boolean hasPermission(String path) {
        String tokenPath = token.getPath();

        if (token.isWildcard()) {
            for (Repository repository : getRepositories()) {
                String name = "/" + repository.getName();

                if (path.startsWith(name)) {
                    path = StringUtils.replaceFirst(path, "/" + repository.getName(), "*");
                    break;
                }
            }
        }

        return path.startsWith(tokenPath);
    }

    public Collection<Repository> getRepositories() {
        if (token.hasMultiaccess()) {
            return repositoryService.getRepositories();
        }

        for (Repository repository : repositoryService.getRepositories()) {
            String name = "/" + repository.getName();

            if (token.getPath().startsWith(name)) {
                return Collections.singletonList(repository);
            }
        }

        return Collections.emptyList();
    }

    public String getAlias() {
        return getToken().getAlias();
    }

    public Token getToken() {
        return token;
    }

}
