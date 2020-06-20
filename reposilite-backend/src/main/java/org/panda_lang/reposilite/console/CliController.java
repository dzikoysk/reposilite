package org.panda_lang.reposilite.console;

import io.javalin.websocket.WsHandler;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteWriter;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.utils.Result;

import java.util.function.Consumer;

public final class CliController implements Consumer<WsHandler> {

    private final Reposilite reposilite;
    private final Authenticator authenticator;
    private final Console console;

    public CliController(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.authenticator = reposilite.getAuthenticator();
        this.console = reposilite.getConsole();
    }

    @Override
    public void accept(WsHandler wsHandler) {
        wsHandler.onConnect(ctx -> {
            Result<Session, String> auth = authenticator.auth(ctx.header("Sec-WebSocket-Protocol"));

            if (!auth.isDefined() || !auth.getValue().isManager()) {
                ctx.send("Unauthorized connection request");
                ctx.session.disconnect();
                return;
            }

            wsHandler.onClose(context -> ReposiliteWriter.getConsumers().remove(context));
            ReposiliteWriter.getConsumers().put(ctx, ctx::send);

            for (String message : ReposiliteWriter.getCache()) {
                ctx.send(message);
            }

            wsHandler.onMessage(context -> reposilite.schedule(() -> console.execute(context.message())));
        });
    }

}
