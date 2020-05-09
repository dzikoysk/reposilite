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

import java.io.File;
import java.util.stream.Stream;

final class RepositoryUtils {

    private RepositoryUtils() { }

    protected static File[] toSortedDirectories(File directory) {
        return Stream.of(directory.listFiles())
                .filter(File::isDirectory)
                .sorted((file, to) -> to.getName().compareTo(file.getName())) // reversed order
                .toArray(File[]::new);
    }

    protected static File getLatest(File[] directories) {
        return directories.length > 0 ? directories[0] : null;
    }

}
