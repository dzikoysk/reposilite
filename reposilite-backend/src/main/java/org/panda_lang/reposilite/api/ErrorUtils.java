package org.panda_lang.reposilite.api;

import io.javalin.http.Context;

public final class ErrorUtils {

    private ErrorUtils() { }

    public static Context error(Context context, int status, String message) {
        return context
                .status(status)
                .json(new ErrorDto(status, message));
    }

}
