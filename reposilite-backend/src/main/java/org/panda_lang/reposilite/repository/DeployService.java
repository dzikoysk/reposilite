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

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.utils.ErrorDto;
import org.panda_lang.reposilite.utils.ResponseUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.function.ThrowingSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public final class DeployService {

    private final Reposilite reposilite;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;
    private final MetadataService metadataService;

    public DeployService(Reposilite reposilite, Authenticator authenticator, RepositoryService repositoryService, MetadataService metadataService) {
        this.reposilite = reposilite;
        this.authenticator = authenticator;
        this.repositoryService = repositoryService;
        this.metadataService = metadataService;
    }

    public Result<FileDto, ErrorDto> deploy(String address, Map<String, String> headers, String uri, ThrowingSupplier<InputStream, IOException> deployedFile) {
        if (!reposilite.getConfiguration().deployEnabled) {
            return ResponseUtils.error(HttpStatus.SC_METHOD_NOT_ALLOWED, "Artifact deployment is disabled");
        }

        Result<Session, String> authResult = this.authenticator.authByUri(headers, uri);

        if (authResult.containsError()) {
            return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, authResult.getError());
        }

        DiskQuota diskQuota = repositoryService.getDiskQuota();

        if (!diskQuota.hasUsableSpace()) {
            return ResponseUtils.error(HttpStatus.SC_INSUFFICIENT_STORAGE, "Out of disk space");
        }

        File file = repositoryService.getFile(uri);
        FileDto fileDto = FileDto.of(file);

        File metadataFile = new File(file.getParentFile(), "maven-metadata.xml");
        metadataService.clearMetadata(metadataFile);

        if (file.getName().contains("maven-metadata")) {
            return Result.ok(fileDto);
        }

        try {
            FileUtils.forceMkdirParent(file);
            Files.copy(Objects.requireNonNull(deployedFile.get()), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            diskQuota.allocate(file.length());

            Reposilite.getLogger().info("DEPLOY " + authResult.getValue().getAlias() + " successfully deployed " + file + " from " + address);
            return Result.ok(fileDto);
        } catch (IOException e) {
            reposilite.throwException(uri, e);
            return ResponseUtils.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to upload artifact");
        }
    }

}
