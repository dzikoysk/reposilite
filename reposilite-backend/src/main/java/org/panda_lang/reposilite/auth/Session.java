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

import org.panda_lang.utilities.commons.StringUtils;

import java.util.Collections;
import java.util.List;

public final class Session {

    private final Token token;

    public Session(Token token) {
        this.token = token;
    }

    public boolean hasPermission(List<String> repositories, String path) {
        String tokenPath = token.getPath();

        if (token.isWildcard()) {
            for (String repository : getRepositories(repositories)) {
                String name = "/" + repository;

                if (path.startsWith(name)) {
                    path = StringUtils.replaceFirst(path, "/" + repository, "*");
                    break;
                }
            }
        }

        return path.startsWith(tokenPath);
    }

    public List<String> getRepositories(List<String> repositories) {
        if (token.isWildcard() || "/".equals(token.getPath())) {
            return repositories;
        }

        for (String repository : repositories) {
            String name = "/" + repository;

            if (token.getPath().startsWith(name)) {
                return Collections.singletonList(repository);
            }
        }

        return Collections.emptyList();
    }

    public Token getToken() {
        return token;
    }

}
