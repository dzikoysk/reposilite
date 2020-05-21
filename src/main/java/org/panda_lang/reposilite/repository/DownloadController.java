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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import org.panda_lang.reposilite.Configuration;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteController;
import org.panda_lang.reposilite.ReposiliteHttpServer;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.Result;
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
    private final HttpRequestFactory requestFactory;

    public DownloadController(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.configuration = reposilite.getConfiguration();
        this.authenticator = reposilite.getAuthenticator();
        this.metadataService = reposilite.getMetadataService();
        this.repositoryService = reposilite.getRepositoryService();
        this.requestFactory = configuration.getProxied().isEmpty() ? null : new NetHttpTransport().createRequestFactory();
    }

    @Override
    public Response serve(ReposiliteHttpServer server, IHTTPSession httpSession) throws Exception {
        Response response = serveLocal(httpSession);

        if (response.getStatus().getRequestStatus() == Status.NOT_FOUND.getRequestStatus()) {
            return serveProxied(httpSession);
        }

        return response;
    }

    public Response serveProxied(IHTTPSession httpSession) throws IOException {
        String uri = httpSession.getUri();

        for (String proxied : configuration.getProxied()) {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(proxied + uri));
            request.setThrowExceptionOnExecuteError(false);
            HttpResponse response = request.execute();

            if (!response.isSuccessStatusCode()) {
                continue;
            }

            return NanoHTTPD.newFixedLengthResponse(
                    Status.lookup(response.getStatusCode()),
                    response.getMediaType().toString(),
                    response.getContent(),
                    response.getContent().available()
            );
        }

        return notFound(reposilite, "Artifact not found in local and remote repository");
    }

    public Response serveLocal(IHTTPSession httpSession) throws IOException {
        if (configuration.isFullAuthEnabled()) {
            Result<Session, Response> authResult = this.authenticator.authUri(httpSession);

            if (this.authenticator.authUri(httpSession).getError().isDefined()) {
                return authResult.getError().get();
            }
        }

        String[] path = RepositoryUtils.normalizeUri(configuration, httpSession.getUri()).split("/");

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

            return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/xml", result);
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
            Response response = NanoHTTPD.newChunkedResponse(Response.Status.OK, mimeType, content);
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

    private Response notFound(Reposilite reposilite, String message) {
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", reposilite.getFrontend().forMessage(message));
    }

}
