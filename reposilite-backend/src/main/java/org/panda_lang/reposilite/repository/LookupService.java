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

import io.javalin.http.Context;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class LookupService {

    private final Authenticator authenticator;
    private final RepositoryAuthenticator repositoryAuthenticator;
    private final MetadataService metadataService;
    private final RepositoryService repositoryService;
    private final FailureService failureService;

    public LookupService(
            Authenticator authenticator,
            RepositoryAuthenticator repositoryAuthenticator,
            MetadataService metadataService,
            RepositoryService repositoryService,
            FailureService failureService) {

        this.authenticator = authenticator;
        this.repositoryAuthenticator = repositoryAuthenticator;
        this.metadataService = metadataService;
        this.repositoryService = repositoryService;
        this.failureService = failureService;
    }

    Result<Context, ErrorDto> findLocal(Context context) {
        String uri = context.req.getRequestURI();
        Result<Pair<String[], Repository>, ErrorDto> result = this.repositoryAuthenticator.authDefaultRepository(context.headerMap(), uri);

        if (result.containsError()) {
            // Maven requests maven-metadata.xml file during deploy for snapshot releases without specifying credentials
            // https://github.com/dzikoysk/reposilite/issues/184
            if (uri.contains("-SNAPSHOT") && uri.endsWith("maven-metadata.xml")) {
                return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, result.getError().getMessage());
            }

            return Result.error(result.getError());
        }

        String[] path = result.getValue().getKey();
        // remove repository name from path
        String[] requestPath = Arrays.copyOfRange(path, 1, path.length);

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (requestPath.length < 2) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Missing artifact identifier");
        }

        Repository repository = result.getValue().getValue();
        String requestedFileName = Objects.requireNonNull(ArrayUtils.getLast(requestPath));

        if (requestedFileName.equals("maven-metadata.xml")) {
            return metadataService
                    .generateMetadata(repository, requestPath)
                    .map(res -> context.contentType("text/xml").result(res))
                    .mapError(error -> new ErrorDto(HttpStatus.SC_USE_PROXY, error));
        }

        // resolve requests for latest version of artifact
        if (requestedFileName.equalsIgnoreCase("latest")) {
            File requestDirectory = repository.getFile(requestPath).getParentFile();
            File[] versions = MetadataUtils.toSortedVersions(requestDirectory);
            File version = ArrayUtils.getFirst(versions);

            if (version == null) {
                return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Latest version not found");
            }

            return Result.ok(context.result(version.getName()));
        }

        // resolve snapshot requests
        if (requestedFileName.contains("-SNAPSHOT")) {
            repositoryService.resolveSnapshot(repository, requestPath);
            // update requested file name in case of snapshot request
            requestedFileName = requestPath[requestPath.length - 1];
        }

        File repositoryFile = repository.getFile(requestPath);

        if (repositoryFile.exists() && repositoryFile.isDirectory()) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Directory access");
        }

        Optional<Artifact> artifact = repository.find(requestPath);

        if (!artifact.isPresent()) {
            return ResponseUtils.error(HttpStatus.SC_USE_PROXY, "Artifact " + requestedFileName + " not found");
        }

        File file = artifact.get().getFile(requestedFileName);
        FileInputStream content = null;

        try {
            // resolve content type associated with the requested extension
            String mimeType = Files.probeContentType(file.toPath());
            context.res.setContentType(mimeType != null ? mimeType : "application/octet-stream");

            // add content description to the header
            context.res.setContentLengthLong(file.length());
            context.res.setHeader("Content-Disposition", "attachment; filename=\"" + ArrayUtils.getLast(path) + "\"");

            // exclude content for head requests
            if (!context.method().equals("HEAD")) {
                content = new FileInputStream(file);
                IOUtils.copy(content, context.res.getOutputStream());
            }

            // success
            Reposilite.getLogger().info("RESOLVED " + file.getPath() + "; mime: " + mimeType + "; size: " + file.length());
            return Result.ok(context);
        } catch (Exception exception) {
            failureService.throwException(context.req.getRequestURI(), exception);
            return ResponseUtils.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Cannot read artifact");
        } finally {
            FilesUtils.close(content);
        }
    }

    FileListDto findAvailableRepositories(Map<String, String> headers) {
        return new FileListDto(repositoryService.getRepositories().stream()
                .filter(repository -> repository.isPublic() || authenticator.authByUri(headers, repository.getUri()).isDefined())
                .map(Repository::getFile)
                .map(FileDto::of)
                .collect(Collectors.toList()));
    }

    Optional<FileDto> findLatest(File requestedFile) {
        if (requestedFile.getName().equals("latest")) {
            File parent = requestedFile.getParentFile();

            if (parent != null && parent.exists()) {
                File[] files = MetadataUtils.toSortedVersions(parent);
                File latest = ArrayUtils.getFirst(files);

                if (latest != null) {
                    return Optional.of(FileDto.of(latest));
                }
            }
        }

        return Optional.empty();
    }



}
