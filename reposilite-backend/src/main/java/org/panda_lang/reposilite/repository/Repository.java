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

import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public final class Repository {

    private final File directory;
    private final String name;
    private final boolean hidden;

    public Repository(File rootDirectory, String name, boolean hidden) {
        this.directory = new File(rootDirectory, name);
        this.name = name;
        this.hidden = hidden;
    }

    public Optional<Artifact> find(String... path) {
        File targetFile = getFile(path);

        if (!targetFile.exists() || targetFile.isDirectory() || path.length < 3) {
            return Optional.empty();
        }

        String groupId = MetadataUtils.toGroup(Arrays.copyOfRange(path, 0, path.length - 3));
        String artifactId = path[path.length - 3];
        String version = path[path.length - 2];

        return Optional.of(new Artifact(this, groupId, artifactId, version));
    }

    public boolean isPublic() {
        return !isHidden();
    }

    public boolean isHidden() {
        return hidden;
    }

    public File getFile(String... path) {
        return new File(directory, ContentJoiner.on(File.separator).join(path).toString());
    }

    public String getUri() {
        return "/" + getName();
    }

    public String getName() {
        return name;
    }

}
