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

package org.panda_lang.reposilite.utils;

import io.javalin.http.Context;

public final class ResponseUtils {

    private ResponseUtils() { }

    public static Context response(Context ctx, Result<?, ErrorDto> response) {
        response.peek(ctx::json).onError(error -> errorResponse(ctx, error));
        return ctx;
    }

    public static <T> Result<T, ErrorDto> error(int status, String messge) {
        return Result.error(new ErrorDto(status, messge));
    }

    public static Context errorResponse(Context context, int status, String message) {
        return errorResponse(context, new ErrorDto(status, message));
    }

    public static Context errorResponse(Context context, ErrorDto error) {
        return context
                .status(error.getStatus())
                .json(error);
    }

}
