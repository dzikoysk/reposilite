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

import org.panda_lang.nanomaven.server.auth.NanoProject;
import org.panda_lang.nanomaven.server.auth.NanoProjectsManager;

import java.io.File;
import java.util.regex.Pattern;

public class NanoRepositoryProject {

    private final String group;
    private final String artifact;
    private final String version;
    private final File[] files;
    private String repository;

    public NanoRepositoryProject(String repository, String group, String artifact, String version) {
        this.repository = repository;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.files = getFile("").listFiles();
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getLocalPath() {
        return group.replace(".", "/") + "/" + artifact + "/" + version + "/";
    }

    public File getFile(String fileName) {
        String[] parts = fileName.split("/");
        return new File("repositories/" + repository + "/" + getLocalPath() + parts[parts.length - 1]);
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

    public String getProjectName() {
        return getGroup() + "/" + getArtifact();
    }

    public NanoProject toNanoProject(NanoProjectsManager projectsManager) {
        return projectsManager.getProject(getProjectName());
    }

    public static NanoRepositoryProject fromPath(String jarPath) {
        String splitRegex = Pattern.quote(System.getProperty("file.separator"));
        String[] sides = jarPath.split(splitRegex + "repositories" + splitRegex);

        String path = sides[sides.length - 1];
        String[] dirs = path.split(splitRegex);

        String repositoryName = dirs[0];
        String versionDir = dirs[dirs.length - 2];
        String artifactId = dirs[dirs.length - 3];

        StringBuilder groupIdBuilder = new StringBuilder(dirs[1]);
        for (int i = 2; i < dirs.length - 3; i++) {
            groupIdBuilder.append(".").append(dirs[i]);
        }
        String groupId = groupIdBuilder.toString();

        return new NanoRepositoryProject(repositoryName, groupId, artifactId, versionDir);
    }

}
