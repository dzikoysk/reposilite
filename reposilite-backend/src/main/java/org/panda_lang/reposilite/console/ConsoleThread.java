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

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.error.FailureService;

import java.io.InputStream;
import java.util.Scanner;

final class ConsoleThread extends Thread {

    private final Console console;
    private final InputStream source;
    private final FailureService failureService;

    ConsoleThread(Console console, InputStream source, FailureService failureService) {
        this.setName("Reposilite | Console Thread");
        this.setDaemon(true);
        this.console = console;
        this.source = source;
        this.failureService = failureService;
    }

    @Override
    public void run() {
        Scanner in = new Scanner(source);

        if (!in.hasNextLine()) {
            console.getLogger().warn("Interactive CLI is not available in current environment.");
            console.getLogger().warn("Solution for Docker users: https://docs.docker.com/engine/reference/run/#foreground");
            return;
        }

        do {
            String command = in.nextLine();

            try {
                console.defaultExecute(command);
            } catch (Exception exception) {
                failureService.throwException("Command: " + command, exception);
            }
        }
        while (!isInterrupted() && in.hasNextLine());
    }

}
