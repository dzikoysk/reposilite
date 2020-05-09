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

package org.panda_lang.nanomaven.repository;

import org.panda_lang.nanomaven.utils.GroupUtils;

import java.io.File;
import java.util.Arrays;

public class Repository {

    private final String repositoryName;
    private final String path;

    public Repository(String repositoryName) {
        this.repositoryName = repositoryName;
        this.path = "repositories/" + (repositoryName.isEmpty() ? "" : repositoryName + "/");
    }

    public Artifact get(String... path) {
        StringBuilder pathBuilder = new StringBuilder(getLocalPath());

        for (String element : path) {
            if (element.isEmpty()) {
                continue;
            }

            pathBuilder.append(element);
            pathBuilder.append("/");
        }

        pathBuilder.setLength(pathBuilder.length() - 1);
        File targetFile = new File(pathBuilder.toString());

        if (!targetFile.getName().equals("maven-metadata.xml") && (!targetFile.exists() || targetFile.isDirectory())) {
            return null;
        }

        return new Artifact(repositoryName, GroupUtils.groupFromArray(Arrays.copyOfRange(path, 0, path.length - 3)), path[path.length - 3], path[path.length - 2]);
    }

    public String getLocalPath() {
        return path;
    }

    public String getDirectory() {
        return getLocalPath();
    }

    public String getRepositoryName() {
        return repositoryName;
    }

}
