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

package org.panda_lang.reposilite.error;

import io.javalin.http.Context;
import io.javalin.http.ExceptionHandler;

public final class FailureHandler implements ExceptionHandler<Exception> {

    private final FailureService failureService;

    public FailureHandler(FailureService failureService) {
        this.failureService = failureService;
    }

    @Override
    public void handle(Exception exception, Context context) {
        failureService.throwException(context.req.getRequestURI(), exception);
    }

}
