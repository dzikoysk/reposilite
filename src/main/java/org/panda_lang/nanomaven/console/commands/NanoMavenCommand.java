/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven.console.commands;

import io.airlift.airline.Command;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.console.NanoCommand;
import org.panda_lang.nanomaven.workspace.repository.NanoRepositoryManager;

public abstract class NanoMavenCommand implements NanoCommand {

    @Command(name = "rs", description = "Repository List")
    public static class RepositoryList extends NanoMavenCommand {

        @Override
        public void call(NanoMaven nanoMaven) {
            NanoRepositoryManager repositoryManager = nanoMaven.getRepositoryManager();
            System.out.println("ls");
        }

    }

    @Command(name = "scan", description = "Repository Scan")
    public static class RepositoryScan extends NanoMavenCommand {

        @Override
        public void call(NanoMaven nanoMaven) {
            NanoRepositoryManager repositoryManager = nanoMaven.getRepositoryManager();
            System.out.println("ls2");
        }

    }

}
