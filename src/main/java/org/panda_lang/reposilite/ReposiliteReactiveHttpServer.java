package org.panda_lang.reposilite;

import org.panda_lang.reposilite.config.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.net.InetSocketAddress;

final class ReposiliteReactiveHttpServer {

    private final Reposilite reposilite;

    ReposiliteReactiveHttpServer(Reposilite reposilite) {
        this.reposilite = reposilite;
    }

    DisposableServer start(Configuration configuration) {
        return HttpServer.create()
                .bindAddress(() -> InetSocketAddress.createUnresolved(configuration.getHostname(), configuration.getPort() + 1))
                .route(routes -> routes
                        .get("/", (request, response) -> response.sendString(Mono.just(reposilite.getFrontend().forMessage("#onlypanda"))))
                        .get("/*", (request, response) -> response)
                        .head("/*", (request, response) -> response)
                        .post("/*", (request, response) -> response)
                )
                .bindNow();
                //.bindUntilJavaShutdown(Duration.ZERO, onStart);
    }

}
