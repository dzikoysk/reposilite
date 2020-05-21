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

import org.panda_lang.reposilite.Configuration;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;

public final class RepositoryUtils {

    private RepositoryUtils() { }

    protected static String normalizeUri(Configuration configuration, String uri) {
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

    public static File toRequestedFile(Repository repository, String[] requestPath) {
        return new File(repository.getLocalPath() + File.separator + ContentJoiner.on(File.separator).join(requestPath));
    }

}
