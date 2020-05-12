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
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XmlRootElement(name = "versions")
@XmlAccessorType(XmlAccessType.FIELD)
final class Versions {

    private final Collection<String> version;

    Versions(Collection<String> version) {
        this.version = version;
    }

    Versions() {
        this.version = new ArrayList<>();
    }

    Collection<String> getVersion() {
        return version;
    }

    static Versions of(File[] files) {
        return new Versions(Stream.of(files)
                .map(File::getName)
                .collect(Collectors.toList()));
    }

}
