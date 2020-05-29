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
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Lazy;

import java.util.function.Supplier;

public final class Frontend {

    private final Lazy<String> index;
    private final Lazy<String> app;

    public Frontend(Supplier<String> index, Supplier<String> app) {
        this.index = new Lazy<>(index);
        this.app = new Lazy<>(app);
    }

    public String forMessage(String message) {
        return StringUtils.replace(index.get(), "{{message}}", message);
    }

    public String getApp() {
        return app.get();
    }

    public static Frontend createInstance() {
        return new Frontend(() -> FilesUtils.getResource("/frontend/index.html"), () -> FilesUtils.getResource("/frontend/js/app.js"));
    }

}
