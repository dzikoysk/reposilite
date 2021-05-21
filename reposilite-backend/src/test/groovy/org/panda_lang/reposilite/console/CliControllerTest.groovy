/*
 * Copyright (c) 2021 dzikoysk
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

package org.panda_lang.reposilite.console

import groovy.transform.CompileStatic
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification

import java.util.function.BiConsumer

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * dirty websocket connection test
 */
@CompileStatic
class CliControllerTest extends ReposiliteIntegrationTestSpecification {

    @Test
    void 'should not authorize invalid credentials' () {
        def session = connect("invalid", "invalid", { s, msg -> /* keep connection */ })
        freeze(session)
        assertFalse session.isOpen()
    }

    @Test
    void 'should authorize and send messages' () {
        def result = super.reposilite.getTokenService().createToken('/', 'admin', 'rwm')
        def output = new StringBuilder()

        def session = connect("admin", result.getKey(), { s, message ->
            output.append(message)

            if (message.contains("Done")) {
                s.close()
            }
        })

        assertTrue session.isOpen()
        freeze(session)
        session.disconnect()
        assertTrue output.toString().contains("Done")
    }

    private static void freeze(Session session) {
        def uptime = System.currentTimeMillis()

        // Don't let JUnit to interrupt this task for up to 2 seconds
        while (session.isOpen() && (System.currentTimeMillis() - uptime) < 2000) {
            //noinspection BusyWait
            Thread.sleep(50)
        }
    }
    
    private Session connect(String username, String password, BiConsumer<Session, String> messageConsumer) throws Exception {
        def webSocketClient = new WebSocketClient()
        webSocketClient.start()

        return webSocketClient.connect(new WebSocketListener() {
            private Session session

            @Override
            void onWebSocketConnect(Session session) {
                this.session = session
                def authCredentials = String.format("Authorization:%s:%s", username, password)

                try {
                    session.getRemote().sendString(authCredentials)
                } catch (IOException iOException) {
                    throw new RuntimeException(iOException)
                }
            }

            @Override
            void onWebSocketText(String message) {
                messageConsumer.accept(session, message)
            }

            @Override
            void onWebSocketError(Throwable cause) {
                throw new RuntimeException(cause)
            }

            @Override
            void onWebSocketBinary(byte[] payload, int offset, int len) { }


            @Override
            void onWebSocketClose(int statusCode, String reason) { }

        }, new URI("ws://localhost:" + PORT + "/api/cli")).get()
    }

}