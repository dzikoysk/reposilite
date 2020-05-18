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

@XmlRootElement(name = "snapshot")
@XmlAccessorType(XmlAccessType.FIELD)
final class Snapshot {

    private String timestamp;
    private String buildNumber;

    Snapshot(String timestamp, String buildNumber) {
        this.timestamp = timestamp;
        this.buildNumber = buildNumber;
    }

    Snapshot() {

    }

    String getBuildNumber() {
        return buildNumber;
    }

    String getTimestamp() {
        return timestamp;
    }

}
