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

import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.utilities.commons.function.Result;

import java.util.Map;

public final class AuthService {

    private final Authenticator authenticator;

    public AuthService(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    Result<AuthDto, ErrorDto> authByHeader(Map<String, String> headers) {
        return authenticator
                .authByHeader(headers)
                .map(session -> new AuthDto(session.getToken().getPath(), session.getToken().getPermissions(), session.getRepositoryNames()))
                .mapErr(error -> new ErrorDto(HttpStatus.SC_UNAUTHORIZED, error));
    }

}
