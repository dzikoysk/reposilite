package org.panda_lang.reposilite.console;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.auth.Token;
import org.panda_lang.utilities.commons.collection.Pair;

import java.net.URI;
import java.util.Collections;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * dirty websocket connection test
 */
class CliControllerTest extends ReposiliteIntegrationTest {

    @Test
    void shouldNotAuthorizeInvalidCredentials() throws Exception {
        Session session = connect("invalid", "invalid", (s, msg) -> { /* keep connection */ });
        freeze(session);
        assertFalse(session.isOpen());
    }

    @Test
    void shouldAuthorizeAndSendMessages() throws Exception {
        Pair<String, Token> result = super.reposilite.getTokenService().createToken("/", "admin");
        super.reposilite.getConfiguration().setManagers(Collections.singletonList("admin"));
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

        ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
        clientUpgradeRequest.setSubProtocols(username + ":" + password);

        return webSocketClient.connect(new WebSocketListener() {
            private Session session;

            @Override
            public void onWebSocketConnect(Session session) {
                this.session = session;
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

        }, new URI("ws://localhost:80/api/cli"), clientUpgradeRequest).get();
    }

}