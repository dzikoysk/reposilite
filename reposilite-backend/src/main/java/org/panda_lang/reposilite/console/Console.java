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
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.utils.Result;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.UnmatchedArgumentException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Command(name = "", version = "Reposilite " + ReposiliteConstants.VERSION)
public final class Console {

    private final ConsoleThread consoleThread;
    private final CommandLine commandExecutor;

    public Console(InputStream source, FailureService failureService) {
        this.consoleThread = new ConsoleThread(this, source, failureService);
        this.commandExecutor = new CommandLine(this);
    }

    public void hook() {
        consoleThread.start();
    }

    public CommandLine registerCommand(ReposiliteCommand command) {
        return commandExecutor.addSubcommand(command);
    }

    public boolean defaultExecute(String command) {
        Reposilite.getLogger().info("");
        boolean status = execute(command, line -> Reposilite.getLogger().info(line));
        Reposilite.getLogger().info("");

        return status;
    }

    public boolean execute(String command, Consumer<String> outputConsumer) {
        Result<List<String>, List<String>> response = execute(command);

        for (String entry : (response.isDefined() ? response.getValue() : response.getError())) {
            for (String line : entry.replace(System.lineSeparator(), "\n").split("\n")) {
                outputConsumer.accept(line);
            }
        }

        return response.isDefined();
    }

    public Result<List<String>, List<String>> execute(String command) {
        command = command.trim();

        if (command.isEmpty()) {
            return Result.error(Collections.emptyList());
        }

        List<String> response = new ArrayList<>();

        try {
            ParseResult result = commandExecutor.parseArgs(command.split(" "));
            Object commandObject = result.subcommand().commandSpec().userObject();

            if (!(commandObject instanceof ReposiliteCommand)) {
                return Result.error(Collections.singletonList(commandExecutor.getUsageMessage()));
            }

            return ((ReposiliteCommand) commandObject).execute(response)
                    ? Result.ok(response)
                    : Result.error(response);
        } catch (UnmatchedArgumentException unmatchedArgumentException) {
            return Result.error(Collections.singletonList("Unknown command " + command));
        } catch (MissingParameterException missingParameterException) {
            response.add(missingParameterException.getMessage());
            response.add("");
            response.add(missingParameterException.getCommandLine().getUsageMessage());
            return Result.error(response);
        }
        /*
        switch (command.toLowerCase()) {
            case "help":
            case "?":
                return new HelpCommand().execute(reposilite);
            case "version":
                return new VersionCommand().execute(reposilite);
            case "status":
                return new StatusCommand().execute(reposilite);
            case "purge":
                return new PurgeCommand().execute(reposilite);
            case "tokens":
                return new TokenListCommand().execute(reposilite);
            case "gc":
                Reposilite.getLogger().info("[Utility Command] Called gc");
                System.gc();
                return true;
            case "stop": 
                reposilite.schedule(reposilite::forceShutdown);
                return true;
            default:
                break;
        }

        String[] elements = command.split(" ");
        command = elements[0];

        switch (command.toLowerCase()) {
            case "stats":
                if (elements.length == 1) {
                    return new StatsCommand(-1).execute(reposilite);
                }

                return Option.attempt(NumberFormatException.class, () -> new StatsCommand(Long.parseLong(elements[1])))
                        .orElseGet(new StatsCommand(elements[1]))
                        .execute(reposilite);
            case "keygen":
                return new KeygenCommand(elements[1], elements[2], ArrayUtils.get(elements, 3).orElseGet("w")).execute(reposilite);
            case "revoke":
                return new RevokeCommand(elements[1]).execute(reposilite);
            default:
                Reposilite.getLogger().warn("Unknown command " + command);
                return false;
        }
         */
    }

    public void stop() {
        consoleThread.interrupt();
    }

    protected CommandLine getCommandExecutor() {
        return commandExecutor;
    }

}
