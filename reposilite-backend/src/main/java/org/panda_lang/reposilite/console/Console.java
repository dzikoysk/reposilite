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

import io.vavr.control.Try;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.auth.KeygenCommand;
import org.panda_lang.reposilite.auth.RevokeCommand;
import org.panda_lang.reposilite.auth.TokensListCommand;
import org.panda_lang.reposilite.metadata.PurgeCommand;
import org.panda_lang.reposilite.stats.StatsCommand;
import org.panda_lang.utilities.commons.ArrayUtils;

public class Console {

    private final Reposilite reposilite;
    private final ConsoleThread consoleThread;

    public Console(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.consoleThread = new ConsoleThread(this);
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
            case "version":
                return displayVersion();
            case "status":
                return displayStatus();
            case "purge":
                return new PurgeCommand().call(reposilite);
            case "tokens":
                return new TokensListCommand().call(reposilite);
            case "gc":
                Reposilite.getLogger().info("[Utility Command] Called gc");
                System.gc();
                return true;
            case "stop": 
                reposilite.schedule(reposilite::shutdown);
                return true;
            default:
                break;
        }

        String[] elements = command.split(" ");
        command = elements[0];

        switch (command.toLowerCase()) {
            case "stats":
                if (elements.length == 1) {
                    return new StatsCommand(-1).call(reposilite);
                }

                return Try.ofSupplier(() -> new StatsCommand(Long.parseLong(elements[1])))
                        .getOrElse(new StatsCommand(elements[1]))
                        .call(reposilite);
            case "keygen":
                return new KeygenCommand(elements[1], elements[2]).call(reposilite);
            case "revoke":
                return new RevokeCommand(elements[1]).call(reposilite);
            default:
                Reposilite.getLogger().warn("Unknown command " + command);
                return false;
        }
    }

    public boolean executeArguments(String[] arguments) {
        if (ArrayUtils.isEmpty(arguments)) {
            return false;
        }

        String command = arguments[0].toLowerCase();

        switch (command) {
            case "help":
                return displayHelp();
            case "version":
                return displayVersion();
            default:
                return false;
        }
    }

    public boolean displayVersion() {
        Reposilite.getLogger().info("Reposilite " + ReposiliteConstants.VERSION);
        return true;
    }

    public boolean displayHelp() {
        return new HelpCommand().call(reposilite);
    }

    public boolean displayStatus() {
        return new StatusCommand().call(reposilite);
    }

    public void stop() {
        consoleThread.interrupt();
    }

}
