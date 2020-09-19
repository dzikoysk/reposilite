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

package org.panda_lang.reposilite;

import io.javalin.http.Context;
import org.panda_lang.utilities.commons.StringUtils;

public final class ReposiliteContextFactory {

    private final String forwardedIpHeader;

    ReposiliteContextFactory(String forwardedIpHeader) {
        this.forwardedIpHeader = forwardedIpHeader;
    }

    public ReposiliteContext create(Context context) {
        String realIp = context.header(forwardedIpHeader);
        String address = StringUtils.isEmpty(realIp) ? realIp : context.ip();

        return new ReposiliteContext(
                context.req.getRequestURI(),
                context.method(),
                address,
                context.headerMap(),
                context.req::getInputStream,
                context.res::getOutputStream
        );
    }

}
