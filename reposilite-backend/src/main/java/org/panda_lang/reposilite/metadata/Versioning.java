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

package org.panda_lang.reposilite.metadata;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Collection;

@JacksonXmlRootElement(localName = "versioning")
final class Versioning {

    private String release;
    private String latest;
    @JacksonXmlElementWrapper(localName = "versions")
    @JacksonXmlProperty(localName = "version")
    private Collection<String> versions;
    private Snapshot snapshot;
    @JacksonXmlElementWrapper(localName = "snapshotVersions")
    @JacksonXmlProperty(localName = "snapshotVersion")
    private Collection<SnapshotVersion> snapshotVersions;
    private String lastUpdated;

    Versioning(String release, String latest, Collection<String> versions, Snapshot snapshot, Collection<SnapshotVersion> snapshotVersions, String lastUpdated) {
        this.release = release;
        this.latest = latest;
        this.versions = versions;
        this.snapshot = snapshot;
        this.snapshotVersions = snapshotVersions;
        this.lastUpdated = lastUpdated;
    }

    Versioning() {

    }

    public String getRelease() {
        return release;
    }

    public String getLatest() {
        return latest;
    }

    public Collection<String> getVersions() {
        return versions;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public Collection<SnapshotVersion> getSnapshotVersions() {
        return snapshotVersions;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

}
