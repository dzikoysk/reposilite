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

package org.panda_lang.reposilite.repository;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import org.panda_lang.reposilite.Configuration;
import org.panda_lang.reposilite.ReposiliteController;
import org.panda_lang.reposilite.ReposiliteHttpServer;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

final class DownloadController implements ReposiliteController {

    private final Reposilite reposilite;
    private final Configuration configuration;
    private final Authenticator authenticator;
    private final MetadataService metadataService;
    private final RepositoryService repositoryService;

    public DownloadController(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.configuration = reposilite.getConfiguration();
        this.authenticator = reposilite.getAuthenticator();
        this.metadataService = reposilite.getMetadataService();
        this.repositoryService = reposilite.getRepositoryService();
    }

    @Override
    public NanoHTTPD.Response serve(ReposiliteHttpServer server, NanoHTTPD.IHTTPSession httpSession) throws IOException {
        if (configuration.isFullAuthEnabled()) {
            Result<Session, Response> authResult = this.authenticator.authUri(httpSession);

            if (this.authenticator.authUri(httpSession).getError().isDefined()) {
                return authResult.getError().get();
            }
        }

        String[] path = normalizeUri(reposilite.getConfiguration(), httpSession.getUri()).split("/");

        if (path.length == 0) {
            return notFound(reposilite, "Unsupported request");
        }

        if (path[0].isEmpty()) {
            path = Arrays.copyOfRange(path, 1, path.length);
        }

        Repository repository = repositoryService.getRepository(path[0]);

        if (repository == null) {
            return notFound(reposilite, "Repository " + path[0] + " not found");
        }

        String[] requestPath = Arrays.copyOfRange(path, 1, path.length);

        if (requestPath.length == 0) {
            return notFound(reposilite, "Missing artifact path");
        }

        String requestedFileName = requestPath[requestPath.length - 1];

        if (requestedFileName.equals("maven-metadata.xml")) {
            String result = metadataService.generateMetadata(repository, requestPath);

            if (result == null) {
                return notFound(reposilite, "Metadata not found");
            }

            return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/xml", metadataService.generateMetadata(repository, requestPath));
        }

        if (requestedFileName.contains("-SNAPSHOT")) {
            repositoryService.resolveSnapshot(repository, requestPath);
        }

        Artifact artifact = repository.get(requestPath);

        if (artifact == null) {
            return notFound(reposilite, "Artifact " + ContentJoiner.on("/").join(requestPath) + " not found");
        }

        File file = artifact.getFile(requestPath[requestPath.length - 1]);

        if (!file.exists()) {
            Reposilite.getLogger().warn("File " + file.getAbsolutePath() + " doesn't exist");
            return notFound(reposilite, "Artifact " + file.getName() + " not found");
        }

        FileInputStream content;

        try {
            content = new FileInputStream(file);

            String mimeType = Files.probeContentType(file.toPath());
            NanoHTTPD.Response response = NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, mimeType, content);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + MetadataUtils.getLast(path) +"\"");
            response.addHeader("Content-Length", String.valueOf(file.length()));

            Reposilite.getLogger().info("Available: " + content.available() + "; mime: " + mimeType + "; size: " + file.length() + "; file: " + file.getPath());
            return response;
        } catch (FileNotFoundException e) {
            Reposilite.getLogger().warn("Cannot read file " + file.getAbsolutePath());
            return notFound(reposilite, "Cannot read artifact");
        } catch (IOException e) {
            e.printStackTrace();
            return notFound(reposilite, "Unknown mime type");
        }
    }

    private String normalizeUri(Configuration configuration, String uri) {
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

    private NanoHTTPD.Response notFound(Reposilite reposilite, String message) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/html", reposilite.getFrontend().forMessage(message));
    }

}
