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
import org.panda_lang.utilities.commons.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public final class Session {

    private final Token token;
    private final List<Repository> repositories;

    public Session(Token token, List<Repository> repositories) {
        this.token = token;
        this.repositories = repositories;
    }

    public boolean isManager() {
        return hasPermission(Permission.MANAGER);
    }

    public boolean hasPermission(Permission permission) {
        return token.getPermissions().contains(permission.getName());
    }

    public boolean hasPermissionTo(String path) {
        String tokenPath = token.getPath();

        if (token.isWildcard()) {
            for (Repository repository : repositories) {
                String name = "/" + repository.getName();

                if (path.startsWith(name)) {
                    path = StringUtils.replaceFirst(path, "/" + repository.getName(), "*");
                    break;
                }
            }
        }

        return path.startsWith(tokenPath);
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public List<String> getRepositoryNames() {
        return repositories.stream()
                .map(Repository::getName)
                .collect(Collectors.toList());
    }

    public String getAlias() {
        return getToken().getAlias();
    }

    public Token getToken() {
        return token;
    }

}
