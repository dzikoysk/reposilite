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

import java.io.Serializable;

public final class Token implements Serializable {

    private String alias;
    private String path;
    private String permissions;
    private String token;

    public Token(String path, String alias, String permissions, String token) {
        this.alias = alias;
        this.path = path;
        this.permissions = permissions;
        this.token = token;
    }

    public Token() {
        // deserialize
    }

    public boolean isWildcard() {
        return path.startsWith("*");
    }

    public boolean hasMultiaccess() {
        return "/".equals(path) || isWildcard();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
