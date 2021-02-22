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

import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.utilities.commons.collection.Pair;
import picocli.CommandLine.Command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

@Command(name = "exceptions", description = "Display all recorded exceptions")
final class ExceptionsCommand implements ReposiliteCommand {

    private final FailureService failureService;

    ExceptionsCommand(FailureService failureService) {
        this.failureService = failureService;
    }

    @Override
    public boolean execute(List<String> output) {
        Collection<? extends Pair<String, Throwable>> exceptions = failureService.getExceptions();

        if (exceptions.isEmpty()) {
            output.add("No exception has occurred yet");
            return true;
        }

        output.add("#");
        output.add("# List of cached exceptions:");
        output.add("#");

        int count = 0;

        for (Pair<String, Throwable> exception : exceptions) {
            output.add("Exception " + (++count) + " at " + exception.getKey());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.getValue().printStackTrace(printWriter);
            output.add(stringWriter.toString());
        }

        return true;
    }

}
