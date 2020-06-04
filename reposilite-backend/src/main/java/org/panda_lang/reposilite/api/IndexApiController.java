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

package org.panda_lang.reposilite.api;

import io.javalin.http.Context;
import io.vavr.collection.Stream;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.reposilite.repository.RepositoryUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;

public final class IndexApiController implements RepositoryController {

    private final Configuration configuration;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;

    public IndexApiController(Reposilite reposilite) {
        this.configuration = reposilite.getConfiguration();
        this.authenticator = reposilite.getAuthenticator();
        this.repositoryService = reposilite.getRepositoryService();
    }

    @Override
    public Context handleContext(Context ctx) {
        Reposilite.getLogger().info(ctx.req.getRequestURI() + " API");

        if (!configuration.isIndexingEnabled()) {
            return ctx.json(new ErrorDto(HttpStatus.SC_LOCKED, "Indexing is disabled"));
        }

        String uri = RepositoryUtils.normalizeUri(configuration, StringUtils.replaceFirst(ctx.req.getRequestURI(), "/api/", StringUtils.EMPTY));

        if (configuration.isFullAuthEnabled() && authenticator.authUri(ctx, uri).getError().isDefined()) {
            return ctx.json(new ErrorDto(HttpStatus.SC_UNAUTHORIZED, "Unauthorized request"));
        }

        File requestedFile = repositoryService.getFile(uri);

        if (requestedFile.getName().equals("latest")) {
            File parent = requestedFile.getParentFile();

            if (parent != null && parent.exists()) {
                File[] files = MetadataUtils.toSortedVersions(parent);
                File latest = ArrayUtils.getLatest(files);

                if (latest != null) {
                    return ctx.json(FileDto.of(latest));
                }
            }
        }

        if (!requestedFile.exists()) {
            return ctx.json(new ErrorDto(HttpStatus.SC_NOT_FOUND, "Not Found"));
        }

        if (requestedFile.isFile()) {
            return ctx.json(FileDto.of(requestedFile));
        }

        return ctx.json(new FileListDto(Stream.of(FilesUtils.listFiles(requestedFile))
                .map(FileDto::of)
                .transform(stream -> MetadataUtils.toSorted(stream, FileDto::getName, FileDto::isDirectory))
                .toJavaList()));
    }

}
