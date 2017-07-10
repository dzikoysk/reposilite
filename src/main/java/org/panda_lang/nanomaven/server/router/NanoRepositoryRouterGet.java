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
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.server.NanoHttpdServer;
import org.panda_lang.nanomaven.server.NanoRouter;
import org.panda_lang.nanomaven.workspace.repository.NanoProject;
import org.panda_lang.nanomaven.workspace.repository.NanoRepository;
import org.panda_lang.nanomaven.workspace.repository.NanoRepositoryManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class NanoRepositoryRouterGet implements NanoRouter {

    @Override
    public NanoHTTPD.Response serve(NanoHttpdServer server, NanoHTTPD.IHTTPSession session) {
        NanoMaven nanoMaven = server.getNanoMaven();
        NanoRepositoryManager repositoryManager = nanoMaven.getRepositoryManager();

        String[] path = session.getUri().replace("maven-metadata", "maven-metadata-local").split("/");
        NanoProject project;

        if (!nanoMaven.getConfiguration().isRepositoryPathEnabled()) {
            project = repositoryManager.find(path);
        }
        else {
            NanoRepository repository = repositoryManager.getRepository(path[0]);

            if (repository == null) {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", "Repository " + path[0] + " not found");
            }

            project = repository.get(Arrays.copyOfRange(path, 1, path.length));
        }

        if (project == null) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", "Artifact not found");
        }

        File file = project.getFile(path[path.length - 1]);

        if (!file.exists()) {
            NanoMaven.getLogger().warn("File " + file.getAbsolutePath() + " doesn't exist");
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", "Artifact not found");
        }

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (fis == null) {
            NanoMaven.getLogger().warn("Cannot read file " + file.getAbsolutePath());
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", "Cannot read artifact");
        }

        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", "Unknown mime type");

        try {
            response = NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, Files.probeContentType(file.toPath()), fis);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName().replace("maven-metadata-local", "maven-metadata") +"\"");
            NanoMaven.getLogger().info("Fis: " + fis.available() + "; mime: " + Files.probeContentType(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

}
