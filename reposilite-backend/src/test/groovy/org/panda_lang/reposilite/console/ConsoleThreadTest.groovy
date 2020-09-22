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

package org.panda_lang.reposilite.console

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.reposilite.ReposiliteWriter
import org.panda_lang.utilities.commons.text.ContentJoiner

import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class ConsoleThreadTest {

    @Test
    void 'should print version message' () {
        executeInput("version")

        assertTrue ContentJoiner.on('')
                .join(ReposiliteWriter.getCache())
                .toString()
                .contains(ReposiliteConstants.VERSION)
    }

    @Test
    void 'should print docker info' () {
        executeInput('')

        assertTrue ContentJoiner.on('')
                .join(ReposiliteWriter.getCache())
                .toString()
                .contains("Docker")
    }

    private static void executeInput(String command) {
        def input = new ByteArrayInputStream(command.getBytes())
        def console = new Console(null, input)
        def thread = new ConsoleThread(console, input)
        thread.run()
    }

}