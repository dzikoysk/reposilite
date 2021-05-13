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
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class LookupService {

    private final RepositoryAuthenticator repositoryAuthenticator;
    private final RepositoryService repositoryService;

    public LookupService(
            RepositoryAuthenticator repositoryAuthenticator,
            RepositoryService repositoryService) {

        this.repositoryAuthenticator = repositoryAuthenticator;
        this.repositoryService = repositoryService;
    }

    boolean exists(ReposiliteContext context) {
        String uri = context.uri();
        Result<Pair<Path, Repository>, ErrorDto> result = this.repositoryAuthenticator.authDefaultRepository(context.headers(), uri);

        if (result.isErr()) {
            // Maven requests maven-metadata.xml file during deploy for snapshot releases without specifying credentials
            // https://github.com/dzikoysk/reposilite/issues/184
            if (uri.contains("-SNAPSHOT") && uri.endsWith("maven-metadata.xml")) {
                return false;
            }

            return false;
        }

        Path path = result.get().getKey();

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (path.getNameCount() < 2) {
            return false;
        }

        Repository repository = result.get().getValue();

        return repository.exists(path);
    }

    Result<LookupResponse, ErrorDto> find(ReposiliteContext context) throws IOException {
        String uri = context.uri();
        Result<Pair<Path, Repository>, ErrorDto> result = this.repositoryAuthenticator.authDefaultRepository(context.headers(), uri);

        if (result.isErr()) {
            // Maven requests maven-metadata.xml file during deploy for snapshot releases without specifying credentials
            // https://github.com/dzikoysk/reposilite/issues/184
            if (uri.contains("-SNAPSHOT") && uri.endsWith("maven-metadata.xml")) {
                return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, result.getError().getMessage());
            }

            return Result.error(result.getError());
        }

        Path path = result.get().getKey();

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (path.getNameCount() < 2) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Missing artifact identifier");
        }

        Repository repository = result.get().getValue();
        String requestedFileName = path.getFileName().toString();

        if (requestedFileName.equals("maven-metadata.xml")) {
            return repository.getFile(path).map(bytes -> new LookupResponse("text/xml", Arrays.toString(bytes)));
        }

        // resolve requests for latest version of artifact
        if (requestedFileName.equalsIgnoreCase("latest")) {
            Path requestDirectory = path.getParent();
            Result<Path[], ErrorDto> versions = MetadataUtils.toSortedVersions(repository, requestDirectory);

            if (versions.isErr()) return versions.map(p -> null);

            Path version = ArrayUtils.getFirst(versions.get());

            if (version == null) {
                return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Latest version not found");
            }

            return Result.ok(new LookupResponse("text/plain", version.getFileName().toString()));
        }

        // resolve snapshot requests
        if (requestedFileName.contains("-SNAPSHOT")) {
            path = repositoryService.resolveSnapshot(repository, path);

            if (path == null) {
                return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "Latest version not found"));
            }
        }

        if (repository.isDirectory(path)) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Directory access");
        }

        Result<byte[], ErrorDto> bytes = repository.getFile(path);

        if (bytes.isErr()) {
            return bytes.map(b -> null);
        }

        Result<FileDetailsDto, ErrorDto> fileDetailsResult = repository.getFileDetails(path);

        if (fileDetailsResult.isOk()) {
            FileDetailsDto fileDetails = fileDetailsResult.get();

            if (!context.method().equals("HEAD")) {
                context.result(outputStream -> outputStream.write(bytes.get()));
            }

            context.getLogger().debug("RESOLVED " + path + "; mime: " + fileDetails.getContentType() + "; size: " + repository.getFileSize(path).get());
            return Result.ok(new LookupResponse(fileDetails));
        } else {
            return Result.error(fileDetailsResult.getError());
        }
    }
}
