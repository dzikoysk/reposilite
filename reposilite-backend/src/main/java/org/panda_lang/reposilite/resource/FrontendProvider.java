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

package org.panda_lang.reposilite.resource;

import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Lazy;
import org.panda_lang.utilities.commons.text.Formatter;

import java.util.function.Supplier;

public final class FrontendProvider {

    private final Lazy<String> index;
    private final Lazy<String> app;

    private FrontendProvider(Supplier<String> index, Supplier<String> app) {
        this.index = new Lazy<>(index);
        this.app = new Lazy<>(app);
    }

    public String forMessage(String message) {
        return StringUtils.replace(index.get(), "{{REPOSILITE.MESSAGE}}", message);
    }

    public String getApp() {
        return app.get();
    }

    public static FrontendProvider load(Configuration configuration) {
        Formatter formatter = new Formatter()
                .register("{{REPOSILITE.BASE_PATH}}", configuration.basePath)
                .register("{{REPOSILITE.VUE_BASE_PATH}}", configuration.basePath.substring(0, configuration.basePath.length() - 1))
                .register("{{REPOSILITE.TITLE}}", configuration.title.replace("'", "\\'"))
                .register("{{REPOSILITE.DESCRIPTION}}", configuration.description.replace("'", "\\'"))
                .register("{{REPOSILITE.ACCENT_COLOR}}", configuration.accentColor);

        return new FrontendProvider(
                () -> formatter.format(FilesUtils.getResource("/static/index.html")),
                () -> formatter.format(FilesUtils.getResource("/static/js/app.js"))
        );
    }

}
