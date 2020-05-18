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

package org.panda_lang.reposilite;

import fi.iki.elonen.NanoHTTPD;
import io.vavr.control.Option;
import org.panda_lang.reposilite.frontend.FrontendController;
import org.panda_lang.reposilite.repository.RepositoryController;
import org.panda_lang.reposilite.temp.TempFileFactory;

import java.io.IOException;

public class ReposiliteHttpServer extends NanoHTTPD {

    private final Reposilite reposilite;
    private final FrontendController frontendController;
    private final RepositoryController repositoryController;

    public ReposiliteHttpServer(Reposilite reposilite) {
        super(reposilite.getConfiguration().getPort());

        this.reposilite = reposilite;
        this.frontendController = new FrontendController(reposilite);
        this.repositoryController = new RepositoryController(reposilite);
    }

    @Override
    public void start() throws IOException {
        super.setTempFileManagerFactory(new TempFileFactory());
        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Reposilite.getLogger().debug(session.getUri() + " | " + session.getParameters().toString() + " | " + session.getMethod() + " | " + session.getQueryParameterString() + " | " + session.getHeaders().toString());

        if (session.getUri().isEmpty() || session.getUri().equals("/") || session.getUri().startsWith("/#")) {
            return frontendController.serve(this, session);
        }

        return repositoryController.serve(this, session);
    }

    public Option<Throwable> getLatestError() {
        return repositoryController.getLatestError();
    }

    public Reposilite getReposilite() {
        return reposilite;
    }

}
