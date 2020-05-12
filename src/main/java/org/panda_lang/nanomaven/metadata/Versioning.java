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

package org.panda_lang.nanomaven.metadata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement(name = "versioning")
@XmlAccessorType(XmlAccessType.FIELD)
final class Versioning {

    private String release;
    private String latest;
    private Versions versions;
    private Snapshot snapshot;
    @XmlElementWrapper(name = "snapshotVersions")
    @XmlElement(name = "snapshotVersion")
    private Collection<SnapshotVersion> snapshotVersions;
    private String lastUpdated;

    Versioning(String release, String latest, Versions versions, Snapshot snapshot, Collection<SnapshotVersion> snapshotVersions, String lastUpdated) {
        this.release = release;
        this.latest = latest;
        this.versions = versions;
        this.snapshot = snapshot;
        this.snapshotVersions = snapshotVersions;
        this.lastUpdated = lastUpdated;
    }

    Versioning() {

    }

    String getRelease() {
        return release;
    }

    String getLatest() {
        return latest;
    }

    Versions getVersions() {
        return versions;
    }

    Snapshot getSnapshot() {
        return snapshot;
    }

    Collection<SnapshotVersion> getSnapshotVersions() {
        return snapshotVersions;
    }

    String getLastUpdated() {
        return lastUpdated;
    }

}
