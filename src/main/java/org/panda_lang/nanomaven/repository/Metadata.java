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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(name = "metadata")
@XmlAccessorType(XmlAccessType.FIELD)
public class Metadata implements Serializable {

    private String groupId;
    private String artifactId;
    private Versioning versioning;

    public Metadata(String groupId, String artifactId, Versioning versioning) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.versioning = versioning;
    }

    public Metadata() {

    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Versioning getVersioning() {
        return versioning;
    }

    @XmlRootElement(name = "versioning")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Versioning {

        private String release;
        private String latest;
        private Versions versions;
        private long lastUpdated;

        public Versioning(String release, String latest, Versions versions, long lastUpdated) {
            this.release = release;
            this.latest = latest;
            this.versions = versions;
            this.lastUpdated = lastUpdated;
        }

        public Versioning() {

        }

        public String getRelease() {
            return release;
        }

        public String getLatest() {
            return latest;
        }

        public Versions getVersions() {
            return versions;
        }

        public long getLastUpdated() {
            return lastUpdated;
        }

        @XmlRootElement(name = "versions")
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class Versions {

            private final Collection<String> version;

            public Versions(Collection<String> version) {
                this.version = version;
            }

            public Versions() {
                this.version = new ArrayList<>();
            }

            public Collection<String> getVersion() {
                return version;
            }

        }

    }

}
