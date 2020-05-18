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

package org.panda_lang.reposilite.frontend;

import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.FileUtils;

import java.io.File;
import java.io.IOException;

public final class FrontendLoader {

    public Frontend loadFrontend(String frontendFile) throws IOException {
        if (!FilesUtils.exists(frontendFile)) {
            FilesUtils.copyResource("/index.html", frontendFile);
        }

        return new Frontend(FileUtils.getContentOfFile(new File(frontendFile)));
    }

}
