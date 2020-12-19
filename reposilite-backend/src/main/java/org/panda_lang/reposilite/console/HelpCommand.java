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

import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.utilities.commons.text.ContentJoiner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Command(name = "help", aliases = "?", helpCommand = true, description = "List of available commands")
final class HelpCommand implements ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "[<command>]", description = "display usage of the given command", defaultValue = "")
    private String requestedCommand;

    private final Console console;

    HelpCommand(Console console) {
        this.console = console;
    }

    @Override
    public boolean execute(List<String> response) {
        Set<CommandLine> uniqueCommands = new TreeSet<>(Comparator.comparing(CommandLine::getCommandName));

        if (!requestedCommand.isEmpty()) {
            CommandLine requested = console.getCommandExecutor().getSubcommands().get(requestedCommand);

            if (requested == null) {
                response.add("Unknown command '" + requestedCommand + "'");
                return false;
            }

            response.add(requested.getUsageMessage());
            return true;
        }

        if (uniqueCommands.isEmpty()) {
            uniqueCommands.addAll(console.getCommandExecutor().getSubcommands().values());
        }

        response.add("Reposilite " + ReposiliteConstants.VERSION + " Commands:");

        for (CommandLine command : uniqueCommands) {
            CommandSpec specification = command.getCommandSpec();

            response.add("  " + command.getCommandName()
                    + " " + ContentJoiner.on(" ").join(specification.args(), ArgSpec::paramLabel)
                    + " - " + ContentJoiner.on(". ").join(specification.usageMessage().description()));
        }

        return true;
    }

}
