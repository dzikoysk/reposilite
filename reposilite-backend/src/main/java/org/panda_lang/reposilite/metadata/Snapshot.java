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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.io.Serializable;

@JacksonXmlRootElement(localName = "snapshot")
final class Snapshot implements Serializable {

    private String timestamp;
    private String buildNumber;

    Snapshot(String timestamp, String buildNumber) {
        this.timestamp = timestamp;
        this.buildNumber = buildNumber;
    }

    Snapshot() {

    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

}
