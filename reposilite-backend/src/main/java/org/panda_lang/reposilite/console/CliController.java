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
                Reposilite.getLogger().info("CLI Unauthorized CLI access request from " + ctx.session.getRemoteAddress());
                ctx.send("Unauthorized connection request");
                ctx.session.disconnect();
                return;
            }

            wsHandler.onClose(context -> ReposiliteWriter.getConsumers().remove(context));
            ReposiliteWriter.getConsumers().put(ctx, ctx::send);
            Reposilite.getLogger().info("CLI " + auth.getValue().getAlias() + " accessed CLI from " + ctx.session.getRemoteAddress());

            for (String message : ReposiliteWriter.getCache()) {
                ctx.send(message);
            }

            wsHandler.onMessage(context -> reposilite.schedule(() -> console.execute(context.message())));
        });
    }

}
