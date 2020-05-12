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

package org.panda_lang.nanomaven.repository;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.NanoHTTPD.ResponseException;
import org.apache.commons.io.FileUtils;
import org.panda_lang.nanomaven.NanoController;
import org.panda_lang.nanomaven.NanoHttpServer;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.auth.Authenticator;
import org.panda_lang.nanomaven.auth.Session;
import org.panda_lang.nanomaven.NanoConfiguration;
import org.panda_lang.nanomaven.metadata.MetadataService;
import org.panda_lang.nanomaven.utils.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class UploadController implements NanoController {

    private final Authenticator authenticator;
    private final MetadataService metadataService;

    public UploadController(Authenticator authenticator, MetadataService metadataService) {
        this.authenticator = authenticator;
        this.metadataService = metadataService;
    }

    @Override
    public NanoHTTPD.Response serve(NanoHttpServer server, NanoHTTPD.IHTTPSession httpSession) throws Exception {
        NanoMaven nanoMaven = server.getNanoMaven();
        NanoConfiguration configuration = nanoMaven.getConfiguration();

        if (!configuration.isDeployEnabled()) {
            return response(Status.INTERNAL_ERROR, "Artifact deployment is disabled");
        }

        Result<Session, Response> authResult = this.authenticator.auth(httpSession);

        if (authResult.getError().isDefined()) {
            return authResult.getError().get();
        }

        Session session = authResult.getValue().get();

        if (!session.hasPermission(httpSession.getUri())) {
            response(Status.UNAUTHORIZED, "Unauthorized access");
        }

        Map<String, String> files = new HashMap<>();

        try {
            httpSession.parseBody(files);
        } catch (IOException | ResponseException e) {
            return response(Status.BAD_REQUEST, "Cannot parse body");
        }

        ArtifactFile targetFile = ArtifactFile.fromURL(httpSession.getUri());

        for (Entry<String, String> entry : files.entrySet()){
            File tempFile = new File(entry.getValue());

            if (tempFile.getName().contains("maven-metadata")) {
                continue;
            }

            FileUtils.forceMkdirParent(targetFile.getFile());
            Files.copy(tempFile.toPath(), targetFile.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        File metadataFile = new File(targetFile.getFile().getParentFile(), "maven-metadata.xml");
        metadataService.clearMetadata(metadataFile);

        return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Success");
    }

    private Response response(Status status, String response) {
        return NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, response);
    }

}
