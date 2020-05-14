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

package org.panda_lang.nanomaven.console;

import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.auth.KeygenCommand;
import org.panda_lang.nanomaven.auth.TokensListCommand;

public class NanoConsole {

    private final NanoMaven nanoMaven;
    private final NanoConsoleThread consoleThread;

    public NanoConsole(NanoMaven nanoMaven) {
        this.nanoMaven = nanoMaven;
        this.consoleThread = new NanoConsoleThread(this);
    }

    public void hook() {
        consoleThread.start();
    }

    // TODO
    public void execute(String command) {
        if (command.trim().isEmpty()) {
            return;
        }

        String[] elements = command.split(" ");

        if (command.equalsIgnoreCase("help")) {
            HelpCommand helpCommand = new HelpCommand();
            helpCommand.call(nanoMaven);
            return;
        }

        if (command.equalsIgnoreCase("stop")) {
            StopCommand stopCommand = new StopCommand();
            stopCommand.call(nanoMaven);
            return;
        }

        if (command.equals("tokens")) {
            TokensListCommand tokensListCommand = new TokensListCommand();
            tokensListCommand.call(nanoMaven);
            return;
        }

        command = elements[0];

        if (command.equalsIgnoreCase("keygen")) {
            KeygenCommand addUserCommand = new KeygenCommand(nanoMaven.getTokenService(), elements[1], elements[2]);
            addUserCommand.call(nanoMaven);
            return;
        }

        NanoMaven.getLogger().warn("Unknown command " + elements[0]);
    }

    public void stop() {
        consoleThread.interrupt();
    }

}
