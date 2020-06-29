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
import io.javalin.http.Handler;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.api.ErrorUtils;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.utils.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class DeployController implements Handler {

    private final Configuration configuration;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;
    private final MetadataService metadataService;

    public DeployController(Reposilite reposilite) {
        this.configuration = reposilite.getConfiguration();
        this.authenticator = reposilite.getAuthenticator();
        this.repositoryService = reposilite.getRepositoryService();
        this.metadataService = reposilite.getMetadataService();
    }

    @Override
    public void handle(Context context) {
        Reposilite.getLogger().info(context.req.getRequestURI() + " DEPLOY");

        deploy(context)
            .onError(error -> ErrorUtils.error(context, HttpStatus.SC_UNAUTHORIZED, error));
    }

    public Result<Context, String> deploy(Context context) {
        if (!configuration.isDeployEnabled()) {
            return Result.error("Artifact deployment is disabled");
        }

        Result<Session, String> authResult = this.authenticator.authDefault(context);

        if (authResult.containsError()) {
            return Result.error(authResult.getError());
        }

        File file = repositoryService.getFile(context.req.getRequestURI());
        File metadataFile = new File(file.getParentFile(), "maven-metadata.xml");
        metadataService.clearMetadata(metadataFile);

        if (file.getName().contains("maven-metadata")) {
            return Result.ok(context.result("Success"));
        }

        try {
            FileUtils.forceMkdirParent(file);
            Files.copy(Objects.requireNonNull(context.req.getInputStream()), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return Result.ok(context.result("Success"));
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("Failed to upload artifact");
        }
    }

}
