/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven;

import org.fusesource.jansi.Ansi.Color;

import static org.fusesource.jansi.Ansi.ansi;

public class NanoMavenConstants {

    public static final String VERSION = "1.0.6";

    public static final boolean INTERNAL_DEBUG = true;

    protected static final String GREETING_MESSAGE = ansi().bold().fg(Color.CYAN).a("NanoMaven ").reset().a(NanoMavenConstants.VERSION).toString();

}
