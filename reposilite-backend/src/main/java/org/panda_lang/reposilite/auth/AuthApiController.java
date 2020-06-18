package org.panda_lang.reposilite.auth;

import io.javalin.http.Context;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.api.ErrorUtils;
import org.panda_lang.reposilite.config.Configuration;

import java.util.List;

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
                .map(session -> {
                    Token token = session.getToken();
                    List<String> repositories;

                    if (token.isWildcard() || "/".equals(token.getPath())) {
                        repositories = configuration.getRepositories();
                    }
                    else {
                        repositories = session.getRepositories(configuration.getRepositories());
                    }

                    return new AuthDto(
                            configuration.getManagers().contains(token.getAlias()),
                            token.getPath(),
                            repositories
                    );
                })
                .map(ctx::json)
                .orElseGet(error -> ErrorUtils.error(ctx, HttpStatus.SC_UNAUTHORIZED, error));
    }

}
