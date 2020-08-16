package org.panda_lang.reposilite.auth;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.http.HttpStatus;

public final class PostAuthHandler implements Handler {

    public static final String WWW_AUTHENTICATE = "www-authenticate";

    @Override
    public void handle(Context context) {
        if (context.status() == HttpStatus.SC_UNAUTHORIZED) {
            context.header(WWW_AUTHENTICATE, "Basic realm=\"Reposilite\", charset=\"UTF-8\"");
        }
    }

}
