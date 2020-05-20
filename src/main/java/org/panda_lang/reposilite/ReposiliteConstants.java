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

package org.panda_lang.reposilite;

import org.fusesource.jansi.Ansi.Color;

import static org.fusesource.jansi.Ansi.ansi;

public final class ReposiliteConstants {

    public static final String VERSION = "2.3.2";

    static final String GREETING_MESSAGE = ansi().bold().fg(Color.GREEN).a("Reposilite ").reset().a(ReposiliteConstants.VERSION).reset().toString();

    public static final String CONFIGURATION_FILE_NAME = "reposilite.yml";

    public static final String TOKENS_FILE_NAME = "tokens.yml";

    public static final String FRONTEND_FILE_NAME = "index.html";

    public static final String TEMP_FILE_NAME = ".temp";

    private ReposiliteConstants() { }

}
