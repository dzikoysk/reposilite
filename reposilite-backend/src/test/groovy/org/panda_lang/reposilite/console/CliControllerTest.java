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

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification;
import org.panda_lang.reposilite.auth.Token;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * dirty websocket connection test
 */
class CliControllerTest extends ReposiliteIntegrationTestSpecification {

    @Test
    void shouldNotAuthorizeInvalidCredentials() throws Exception {
        Session session = connect("invalid", "invalid", (s, msg) -> { /* keep connection */ });
        freeze(session);
        assertFalse(session.isOpen());
    }

    @Test
    void shouldAuthorizeAndSendMessages() throws Exception {
        Pair<String, Token> result = super.reposilite.getTokenService().createToken("/", "admin");
        super.reposilite.getConfiguration().managers = Collections.singletonList("admin");
        StringBuilder output = new StringBuilder();

        Session session = connect("admin", result.getKey(), (s, message) -> {
            output.append(message);

            if (message.contains("Done")) {
                s.close();
            }
        });

        assertTrue(session.isOpen());
        freeze(session);
        session.disconnect();
        assertTrue(output.toString().contains("Done"));
    }

    private void freeze(Session session) throws Exception{
        long uptime = System.currentTimeMillis();

        // Don't let JUnit to interrupt this task for up to 2 seconds
        while (session.isOpen() && (System.currentTimeMillis() - uptime) < 2000) {
            //noinspection BusyWait
            Thread.sleep(50);
        }
    }
    
    private Session connect(String username, String password, BiConsumer<Session, String> messageConsumer) throws Exception {
        WebSocketClient webSocketClient = new WebSocketClient();
        webSocketClient.start();

        return webSocketClient.connect(new WebSocketListener() {
            private Session session;

            @Override
            public void onWebSocketConnect(Session session) {
                this.session = session;

                String authCredentials = String.format("Authorization:%s:%s", username, password);

                try {
                    session.getRemote().sendString(authCredentials);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onWebSocketText(String message) {
                messageConsumer.accept(session, message);
            }

            @Override
            public void onWebSocketError(Throwable cause) {
                throw new RuntimeException(cause);
            }

            @Override
            public void onWebSocketBinary(byte[] payload, int offset, int len) { }


            @Override
            public void onWebSocketClose(int statusCode, String reason) { }

        }, new URI("ws://localhost:" + PORT + "/api/cli")).get();
    }

}