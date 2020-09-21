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

package org.panda_lang.reposilite.console;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification;
import org.panda_lang.utilities.commons.StringUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleTest extends ReposiliteIntegrationTestSpecification {

    @Test
    void shouldFalseOnEmpty() {
        assertFalse(super.reposilite.getConsole().execute(StringUtils.EMPTY));
    }

    @Test
    void shouldSucceedOnSingleElement() {
        Console console = super.reposilite.getConsole();
        assertTrue(console.execute("help"));
        assertTrue(console.execute("version"));
        assertTrue(console.execute("status"));
        assertTrue(console.execute("purge"));
        assertTrue(console.execute("tokens"));
        assertTrue(console.execute("gc"));
        assertTrue(console.execute("stop"));
    }

    @Test
    void shouldSucceedOnComplex() {
        Console console = super.reposilite.getConsole();

        assertTrue(console.execute("stats"));
        assertTrue(console.execute("stats 10"));
        assertTrue(console.execute("stats /"));

        assertTrue(console.execute("keygen / root"));
        assertTrue(console.execute("revoke root"));
    }

    @Test
    void shouldFalseOnUnknown() {
        assertFalse(super.reposilite.getConsole().execute("unknown"));
    }

}