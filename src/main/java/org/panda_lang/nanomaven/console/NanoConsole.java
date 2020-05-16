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

    public boolean execute(String command) {
        if (command.trim().isEmpty()) {
            return false;
        }

        switch (command.toLowerCase()) {
            case "help":
                return displayHelp();
            case "status":
                return new StatusCommand().call(nanoMaven);
            case "purge":
                return new PurgeCommand().call(nanoMaven);
            case "tokens":
                return new TokensListCommand().call(nanoMaven);
            case "gc":
                NanoMaven.getLogger().info("[Utility Command] Called gc");
                System.gc();
                return true;
            case "stop": 
                nanoMaven.shutdown();
                break;
            default:
                break;
        }

        String[] elements = command.split(" ");
        command = elements[0];

        if (command.equalsIgnoreCase("keygen")) {
            KeygenCommand addUserCommand = new KeygenCommand(nanoMaven.getTokenService(), elements[1], elements[2]);
            return addUserCommand.call(nanoMaven);
        }

        NanoMaven.getLogger().warn("Unknown command " + command);
        return false;
    }

    public boolean displayHelp() {
        HelpCommand helpCommand = new HelpCommand();
        return helpCommand.call(nanoMaven);
    }

    public void stop() {
        consoleThread.interrupt();
    }

}
