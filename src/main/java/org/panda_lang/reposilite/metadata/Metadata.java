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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "metadata")
@XmlAccessorType(XmlAccessType.FIELD)
final class Metadata implements Serializable {

    private String groupId;
    private String artifactId;
    private String version;
    private Versioning versioning;

    Metadata(String groupId, String artifactId, String version, Versioning versioning) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.versioning = versioning;
    }

    Metadata() {

    }

    String getGroupId() {
        return groupId;
    }

    String getArtifactId() {
        return artifactId;
    }

    String getVersion() {
        return version;
    }

    Versioning getVersioning() {
        return versioning;
    }

}
