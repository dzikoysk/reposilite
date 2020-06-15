package org.panda_lang.reposilite.api;

import io.javalin.http.Context;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.config.Configuration;

public final class AuthApiController implements RepositoryController {

    private final Configuration configuration;
    private final Authenticator authenticator;

    public AuthApiController(Configuration configuration, Authenticator authenticator) {
        this.configuration = configuration;
        this.authenticator = authenticator;
    }

    @Override
    public Context handleContext(Context ctx) {
        return authenticator
                .auth(ctx)
                .map(Session::getToken)
                .map(token -> new AuthDto(token.getPath(), configuration.getManagers().contains(token.getAlias())))
                .map(ctx::json)
                .orElseGet(error -> ctx.status(HttpStatus.SC_UNAUTHORIZED).json(new ErrorDto(HttpStatus.SC_UNAUTHORIZED, error)));
    }


}
