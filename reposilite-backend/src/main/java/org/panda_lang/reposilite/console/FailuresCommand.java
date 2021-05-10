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
import picocli.CommandLine.Command;

import java.util.Collections;
import java.util.List;

@Command(name = "failures", description = "Display all recorded exceptions")
public final class FailuresCommand implements ReposiliteCommand {

    private final FailureService failureService;

    FailuresCommand(FailureService failureService) {
        this.failureService = failureService;
    }

    @Override
    public boolean execute(List<String> output) {
        if (!failureService.hasFailures()) {
            output.add("No exception has occurred yet");
            return true;
        }

        output.add("");
        output.add("List of cached failures: " + "(" + failureService.getFailures().size() + ")");
        output.add("");

        failureService.getFailures().stream()
                .map(failure -> failure.split(System.lineSeparator()))
                .forEach(lines -> Collections.addAll(output, lines));

        return true;
    }

}
