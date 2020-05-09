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

package org.panda_lang.nanomaven;

import fi.iki.elonen.NanoHTTPD;
import org.panda_lang.nanomaven.frontend.FrontendController;
import org.panda_lang.nanomaven.repository.RepositoryController;
import org.panda_lang.nanomaven.temp.TempFileFactory;

import java.io.IOException;

public class NanoHttpServer extends NanoHTTPD {

    private final NanoMaven nanoMaven;
    private final FrontendController frontRouter;
    private final RepositoryController repositoryRouter;

    public NanoHttpServer(NanoMaven nanoMaven) {
        super(nanoMaven.getConfiguration().getPort());

        this.nanoMaven = nanoMaven;
        this.frontRouter = new FrontendController(nanoMaven);
        this.repositoryRouter = new RepositoryController(nanoMaven);
    }

    @Override
    public void start() throws IOException {
        super.setTempFileManagerFactory(new TempFileFactory());
        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        NanoMaven.getLogger().debug(session.getUri() + " | " + session.getParameters().toString() + " | " + session.getMethod() + " | " + session.getQueryParameterString() + " | " + session.getHeaders().toString());

        if (session.getUri().isEmpty() || session.getUri().equals("/") || session.getUri().startsWith("/#")) {
            return frontRouter.serve(this, session);
        }

        return repositoryRouter.serve(this, session);
    }

    public NanoMaven getNanoMaven() {
        return nanoMaven;
    }

}
