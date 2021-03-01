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

import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class LookupService {

    private final RepositoryAuthenticator repositoryAuthenticator;
    private final MetadataService metadataService;
    private final RepositoryService repositoryService;

    public LookupService(
            RepositoryAuthenticator repositoryAuthenticator,
            MetadataService metadataService,
            RepositoryService repositoryService) {

        this.repositoryAuthenticator = repositoryAuthenticator;
        this.metadataService = metadataService;
        this.repositoryService = repositoryService;
    }

    Result<LookupResponse, ErrorDto> findLocal(ReposiliteContext context) throws IOException {
        String uri = context.uri();
        Result<Pair<String[], Repository>, ErrorDto> result = this.repositoryAuthenticator.authDefaultRepository(context.headers(), uri);

        if (result.isErr()) {
            // Maven requests maven-metadata.xml file during deploy for snapshot releases without specifying credentials
            // https://github.com/dzikoysk/reposilite/issues/184
            if (uri.contains("-SNAPSHOT") && uri.endsWith("maven-metadata.xml")) {
                return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, result.getError().getMessage());
            }

            return Result.error(result.getError());
        }

        String[] path = result.get().getKey();
        // remove repository name from path
        String[] requestPath = Arrays.copyOfRange(path, 1, path.length);

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (requestPath.length < 2) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Missing artifact identifier");
        }

        Repository repository = result.get().getValue();
        String requestedFileName = Objects.requireNonNull(ArrayUtils.getLast(requestPath));

        if (requestedFileName.equals("maven-metadata.xml")) {
            return metadataService
                    .generateMetadata(repository, requestPath)
                    .mapErr(error -> new ErrorDto(HttpStatus.SC_USE_PROXY, error))
                    .map(metadataContent -> new LookupResponse("text/xml", metadataContent));
        }

        // resolve requests for latest version of artifact
        if (requestedFileName.equalsIgnoreCase("latest")) {
            Path requestDirectory = repository.getFile(requestPath).getParent();
            Path[] versions = MetadataUtils.toSortedVersions(requestDirectory);
            Path version = ArrayUtils.getFirst(versions);

            if (version == null) {
                return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Latest version not found");
            }

            return Result.ok(new LookupResponse("text/plain", version.getFileName().toString()));
        }

        // resolve snapshot requests
        if (requestedFileName.contains("-SNAPSHOT")) {
            repositoryService.resolveSnapshot(repository, requestPath);
            // update requested file name in case of snapshot request
            requestedFileName = requestPath[requestPath.length - 1];
        }

        Path repositoryFile = repository.getFile(requestPath);

        if (Files.exists(repositoryFile) && Files.isDirectory(repositoryFile)) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Directory access");
        }

        Optional<Artifact> artifact = repository.find(requestPath);

        if (!artifact.isPresent()) {
            return ResponseUtils.error(HttpStatus.SC_USE_PROXY, "Artifact " + requestedFileName + " not found");
        }

        Path file = artifact.get().getFile(requestedFileName);
        FileDetailsDto fileDetails = FileDetailsDto.of(file);

        if (!context.method().equals("HEAD")) {
            context.result(outputStream -> Files.copy(file, outputStream));
        }

        Reposilite.getLogger().info("RESOLVED " + file + "; mime: " + fileDetails.getContentType() + "; size: " + Files.size(file));
        return Result.ok(new LookupResponse(fileDetails));
    }

}
