/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven.workspace.repository;

import java.io.File;

public class NanoFile {

    private final String fileName;
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String repositoryName;
    private final File directory;
    private final File file;

    public NanoFile(String fileName, String artifactId, String groupId, String version, String repositoryName, File directory, File file) {
        this.fileName = fileName;
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.repositoryName = repositoryName;
        this.directory = directory;
        this.file = file;
    }

    public NanoProject toNanoProject() {
        return new NanoProject(repositoryName, groupId, artifactId, version);
    }

    public File getFile() {
        return file;
    }

    public File getDirectory() {
        return directory;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getFileName() {
        return fileName;
    }

    public static NanoFile fromURL(String urlPath) {
        String[] dirs = urlPath.split("/");

        String repositoryName = dirs[0];
        String fileName = dirs[dirs.length - 1];
        String versionDir = dirs[dirs.length - 2];
        String artifactId = dirs[dirs.length - 3];

        StringBuilder groupIdBuilder = new StringBuilder(dirs[1]);
        for (int i = 2; i < dirs.length - 3; i++) {
            groupIdBuilder.append(".").append(dirs[i]);
        }
        String groupId = groupIdBuilder.toString();

        File file = new File("repositories/" + urlPath);
        File directory = file.getParentFile();

        return new NanoFile(fileName, artifactId, groupId, versionDir, repositoryName, directory, file);
    }

}
