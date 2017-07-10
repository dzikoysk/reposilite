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

package org.panda_lang.nanomaven.server.router;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.panda_lang.nanomaven.server.NanoHttpdServer;
import org.panda_lang.nanomaven.server.NanoRouter;
import org.panda_lang.nanomaven.workspace.repository.NanoFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class NanoRepositoryRouterPut implements NanoRouter {

    @Override
    public NanoHTTPD.Response serve(NanoHttpdServer server, NanoHTTPD.IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            for (Map.Entry<String, String> entry : files.entrySet()){
                File tempFile = new File(entry.getValue());
                NanoFile targetFile = NanoFile.fromURL(session.getUri());

                FileUtils.forceMkdirParent(targetFile.getFile());
                Files.copy(tempFile.toPath(), targetFile.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/html", "Cannot upload artifact");
        }

        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Success");
    }

}
