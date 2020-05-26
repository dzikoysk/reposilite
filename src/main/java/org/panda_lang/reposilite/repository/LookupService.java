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
import io.javalin.http.Context;
import org.apache.commons.io.IOUtils;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

public final class LookupService {

    private final Configuration configuration;
    private final Authenticator authenticator;
    private final MetadataService metadataService;
    private final RepositoryService repositoryService;
    private final HttpRequestFactory requestFactory;

    public LookupService(Reposilite reposilite) {
        this.configuration = reposilite.getConfiguration();
        this.authenticator = reposilite.getAuthenticator();
        this.metadataService = reposilite.getMetadataService();
        this.repositoryService = reposilite.getRepositoryService();
        this.requestFactory = configuration.getProxied().isEmpty() ? null : new NetHttpTransport().createRequestFactory();
    }

    Result<Context, String> serveProxied(Context context) {
        String uri = context.req.getRequestURI();

        for (String proxied : configuration.getProxied()) {
            try {
                HttpRequest remoteRequest = requestFactory.buildGetRequest(new GenericUrl(proxied + uri));
                remoteRequest.setThrowExceptionOnExecuteError(false);
                HttpResponse remoteResponse = remoteRequest.execute();

                if (!remoteResponse.isSuccessStatusCode()) {
                    continue;
                }

                return Result.ok(context.status(remoteResponse.getStatusCode())
                        .contentType(remoteResponse.getContentType())
                        .header("Content-Length", Objects.toString(remoteResponse.getHeaders().getContentLength(), "0"))
                        .result(remoteResponse.getContent()));
            } catch (IOException e) {
                Reposilite.getLogger().warn("Proxied repository " + proxied + " is unavailable: " + e.getMessage());
            }
        }

        return Result.error("Artifact not found in local and remote repository");
    }

    Result<Context, String> serveLocal(Context context) {
        if (configuration.isFullAuthEnabled()) {
            Result<Session, String> authResult = this.authenticator.authUri(context);

            if (authResult.getError().isDefined()) {
                return Result.error(authResult.getError().get());
            }
        }

        String[] path = RepositoryUtils.normalizeUri(configuration, context.req.getRequestURI()).split("/");

        // discard invalid requests (less than 'repository/group/artifact')
        if (path.length < 3) {
            return Result.error("Unsupported request");
        }

        // remove empty element '/x/y/z' before root due to String#split result
        if (path[0].isEmpty()) {
            path = Arrays.copyOfRange(path, 1, path.length);
        }

        Repository repository = repositoryService.getRepository(path[0]);

        if (repository == null) {
            return Result.error("Repository " + path[0] + " not found");
        }

        // remove repository name from path
        String[] requestPath = Arrays.copyOfRange(path, 1, path.length);

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (requestPath.length < 2) {
            return Result.error("Missing artifact path");
        }

        String requestedFileName = requestPath[requestPath.length - 1];

        if (requestedFileName.equals("maven-metadata.xml")) {
            try {
                String result = metadataService.generateMetadata(repository, requestPath);

                if (result == null) {
                    return Result.error("Metadata not found");
                }

                return Result.ok(context.contentType("text/xml").result(result));
            } catch (IOException e) {
                return Result.error("Cannot find metadata file");
            }
        }

        // resolve snapshot requests
        if (requestedFileName.contains("-SNAPSHOT")) {
            // ignore inspection to make it clear that 'requestPath' content is updated
            // noinspection ConstantConditions
            requestPath = repositoryService.resolveSnapshot(repository, requestPath);
        }

        Artifact artifact = repository.get(requestPath);

        if (artifact == null) {
            return Result.error("Artifact " + ContentJoiner.on("/").join(requestPath) + " not found");
        }

        requestedFileName = requestPath[requestPath.length - 1];
        File file = artifact.getFile(requestedFileName);

        if (!file.exists()) {
            Reposilite.getLogger().warn("File " + file.getAbsolutePath() + " does not exist");
            return Result.error("Artifact " + file.getName() + " not found");
        }

        FileInputStream content = null;

        try {
            String mimeType = Files.probeContentType(file.toPath());
            context.res.setContentType(mimeType);

            content = new FileInputStream(file);
            IOUtils.copy(content, context.res.getOutputStream());

            context.res.setContentLength((int) file.length());
            context.res.setHeader("Content-Disposition", "attachment; filename=\"" + MetadataUtils.getLast(path) + "\"");

            Reposilite.getLogger().info("Available: " + content.available() + "; mime: " + mimeType + "; size: " + file.length() + "; file: " + file.getPath());
            return Result.ok(context.header("Content-Disposition", "attachment; filename=\"" + MetadataUtils.getLast(path) + "\""));
        } catch (FileNotFoundException e) {
            Reposilite.getLogger().warn("Cannot read file " + file.getAbsolutePath());
            return Result.error("Cannot read artifact");
        } catch (IOException e) {
            return Result.error("Unknown mime type " + file.getName());
        } finally {
            FilesUtils.close(content);
        }
    }

}
