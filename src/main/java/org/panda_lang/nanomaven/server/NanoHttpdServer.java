/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven.server;

import fi.iki.elonen.NanoHTTPD;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.workspace.data.temp.NanoTempFileFactory;
import org.panda_lang.nanomaven.server.router.NanoFrontRouter;
import org.panda_lang.nanomaven.server.router.NanoPanelRouter;
import org.panda_lang.nanomaven.server.router.NanoRepositoryRouter;

import java.io.IOException;

public class NanoHttpdServer extends NanoHTTPD {

    private final NanoMaven nanoMaven;
    private final NanoFrontRouter frontRouter;
    private final NanoPanelRouter panelRouter;
    private final NanoRepositoryRouter repositoryRouter;

    public NanoHttpdServer(NanoMaven nanoMaven) {
        super(nanoMaven.getConfiguration().getPort());

        this.nanoMaven = nanoMaven;
        this.frontRouter = new NanoFrontRouter(nanoMaven);
        this.panelRouter = new NanoPanelRouter(nanoMaven);
        this.repositoryRouter = new NanoRepositoryRouter(nanoMaven);
    }

    @Override
    public void start() throws IOException {
        super.setTempFileManagerFactory(new NanoTempFileFactory());
        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        NanoMaven.getLogger().debug(session.getUri() + " | " + session.getParameters().toString() + " | " + session.getMethod() + " | " + session.getQueryParameterString() + " | " + session.getHeaders().toString());

        if (session.getUri().isEmpty() || session.getUri().equals("/")) {
            return frontRouter.serve(this, session);
        }
        else if (session.getUri().startsWith("/#")) {
            return panelRouter.serve(this, session);
        }

        return repositoryRouter.serve(this, session);
    }

    public NanoMaven getNanoMaven() {
        return nanoMaven;
    }

}
