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
import org.panda_lang.nanomaven.NanoController;
import org.panda_lang.nanomaven.NanoHttpServer;
import org.panda_lang.nanomaven.NanoMaven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class DownloadController implements NanoController {

    @Override
    public NanoHTTPD.Response serve(NanoHttpServer server, NanoHTTPD.IHTTPSession session) {
        NanoMaven nanoMaven = server.getNanoMaven();
        RepositoryService repositoryService = nanoMaven.getRepositoryService();
        String uri = session.getUri();

        if (!nanoMaven.getConfiguration().isRepositoryPathEnabled()) {
            String mainRepository = nanoMaven.getConfiguration().getRepositories().get(0);

            if (!uri.startsWith("/" + mainRepository)) {
                uri = nanoMaven.getConfiguration().getRepositories().get(0) + "/" + uri;
            }
        }

        String[] path = uri.replace("maven-metadata", "maven-metadata-local").split("/");

        if (path[0].isEmpty()) {
            path = Arrays.copyOfRange(path, 1, path.length);
        }

        Repository repository = repositoryService.getRepository(path[0]);

        if (repository == null) {
            return notFound(nanoMaven, "Repository " + path[0] + " not found");
        }

        Artifact artifact = repository.get(Arrays.copyOfRange(path, 1, path.length));

        if (artifact == null) {
            return notFound(nanoMaven, "Artifact not found");
        }

        File file = artifact.getFile(path[path.length - 1]);

        if (!file.exists()) {
            NanoMaven.getLogger().warn("File " + file.getAbsolutePath() + " doesn't exist");
            return notFound(nanoMaven, "Artifact " + file.getName() + " not found");
        }

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (fis == null) {
            NanoMaven.getLogger().warn("Cannot read file " + file.getAbsolutePath());
            return notFound(nanoMaven, "Cannot read artifact");
        }

        try {
            NanoHTTPD.Response response = NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, Files.probeContentType(file.toPath()), fis);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName().replace("maven-metadata-local", "maven-metadata") +"\"");
            response.addHeader("Content-Length", String.valueOf(file.length()));
            NanoMaven.getLogger().info("Fis: " + fis.available() + "; mime: " + Files.probeContentType(file.toPath()));
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return notFound(nanoMaven, "Unknown mime type");
        }
    }

    private NanoHTTPD.Response notFound(NanoMaven nanoMaven, String message) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", nanoMaven.getFrontend().forMessage(message));
    }

}
