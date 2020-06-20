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
import org.panda_lang.reposilite.utils.ArrayUtils;

import java.io.File;

final class Artifact {

    private final Repository repository;
    private final String group;
    private final String artifact;
    private final String version;
    private final File[] builds;

    Artifact(Repository repository, String group, String artifact, String version) {
        this.repository = repository;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.builds = MetadataUtils.toFiles(getFile(""));
    }

    public File getFile(String fileName) {
        return repository.getFile(getLocalPath() + ArrayUtils.getLast(fileName.split("/")));
    }

    public File getLatest() {
        return ArrayUtils.getLatest(builds);
    }

    public String getLocalPath() {
        return group.replace(".", "/") + "/" + artifact + "/" + version + "/";
    }

    public File[] getBuilds() {
        return builds;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getGroup() {
        return group;
    }

    public Repository getRepository() {
        return repository;
    }

}
