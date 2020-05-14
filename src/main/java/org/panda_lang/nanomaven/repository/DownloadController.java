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
import org.panda_lang.nanomaven.NanoConfiguration;
import org.panda_lang.nanomaven.NanoController;
import org.panda_lang.nanomaven.NanoHttpServer;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.auth.Authenticator;
import org.panda_lang.nanomaven.auth.Session;
import org.panda_lang.nanomaven.metadata.MetadataService;
import org.panda_lang.nanomaven.metadata.MetadataUtils;
import org.panda_lang.nanomaven.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class DownloadController implements NanoController {

    private final NanoMaven nanoMaven;
    private final NanoConfiguration configuration;
    private final Authenticator authenticator;
    private final MetadataService metadataService;
    private final RepositoryService repositoryService;

    public DownloadController(NanoMaven nanoMaven) {
        this.nanoMaven = nanoMaven;
        this.configuration = nanoMaven.getConfiguration();
        this.authenticator = nanoMaven.getAuthenticator();
        this.metadataService = nanoMaven.getMetadataService();
        this.repositoryService = nanoMaven.getRepositoryService();
    }

    @Override
    public NanoHTTPD.Response serve(NanoHttpServer server, NanoHTTPD.IHTTPSession httpSession) throws IOException {
        if (configuration.isFullAuthEnabled()) {
            Result<Session, Response> authResult = this.authenticator.authUri(httpSession);

            if (this.authenticator.authUri(httpSession).getError().isDefined()) {
                return authResult.getError().get();
            }
        }

        String[] path = normalizeUri(nanoMaven.getConfiguration(), httpSession.getUri()).split("/");

        if (path.length == 0) {
            return notFound(nanoMaven, "Unsupported request");
        }

        if (path[0].isEmpty()) {
            path = Arrays.copyOfRange(path, 1, path.length);
        }

        Repository repository = repositoryService.getRepository(path[0]);

        if (repository == null) {
            return notFound(nanoMaven, "Repository " + path[0] + " not found");
        }

        String[] requestPath = Arrays.copyOfRange(path, 1, path.length);

        if (requestPath.length == 0) {
            return notFound(nanoMaven, "Missing artifact path");
        }

        String requestedFileName = requestPath[requestPath.length - 1];

        if (requestedFileName.equals("maven-metadata.xml")) {
            String result = metadataService.generateMetadata(repository, requestPath);

            if (result == null) {
                return notFound(nanoMaven, "Metadata not found");
            }

            return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/xml", metadataService.generateMetadata(repository, requestPath));
        }

        if (requestedFileName.contains("-SNAPSHOT")) {
            requestPath = repositoryService.resolveSnapshot(repository, requestPath);
        }

        Artifact artifact = repository.get(requestPath);

        if (artifact == null) {
            return notFound(nanoMaven, "Artifact " + ContentJoiner.on("/").join(requestPath) + " not found");
        }

        File file = artifact.getFile(requestPath[requestPath.length - 1]);

        if (!file.exists()) {
            NanoMaven.getLogger().warn("File " + file.getAbsolutePath() + " doesn't exist");
            return notFound(nanoMaven, "Artifact " + file.getName() + " not found");
        }

        FileInputStream content;

        try {
            content = new FileInputStream(file);

            String mimeType = Files.probeContentType(file.toPath());
            NanoHTTPD.Response response = NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, mimeType, content);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + MetadataUtils.getLast(path) +"\"");
            response.addHeader("Content-Length", String.valueOf(file.length()));

            NanoMaven.getLogger().info("Available: " + content.available() + "; mime: " + mimeType + "; size: " + file.length() + "; file: " + file.getPath());
            return response;
        } catch (FileNotFoundException e) {
            NanoMaven.getLogger().warn("Cannot read file " + file.getAbsolutePath());
            return notFound(nanoMaven, "Cannot read artifact");
        } catch (IOException e) {
            e.printStackTrace();
            return notFound(nanoMaven, "Unknown mime type");
        }
    }

    private String normalizeUri(NanoConfiguration configuration, String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        if (uri.contains("..")) {
            return StringUtils.EMPTY;
        }

        if (!configuration.isRewritePathsEnabled()) {
            return uri;
        }

        if (StringUtils.countOccurrences(uri, "/") <= 1) {
            return uri;
        }

        for (String repositoryName : configuration.getRepositories()) {
            if (uri.startsWith(repositoryName)) {
                return uri;
            }
        }

        return configuration.getRepositories().get(0) + "/" + uri;
    }

    private NanoHTTPD.Response notFound(NanoMaven nanoMaven, String message) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", nanoMaven.getFrontend().forMessage(message));
    }

}
