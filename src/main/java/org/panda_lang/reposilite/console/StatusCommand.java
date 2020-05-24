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
import org.panda_lang.reposilite.utils.TimeUtils;
import org.panda_lang.utilities.commons.console.Effect;

import java.util.Collection;

final class StatusCommand implements NanoCommand {

    @Override
    public boolean call(Reposilite reposilite) {
        Reposilite.getLogger().info("");
        Reposilite.getLogger().info("Reposilite " + ReposiliteConstants.VERSION + " Status");
        Reposilite.getLogger().info("  Active: " + Effect.GREEN_BOLD + reposilite.getHttpServer().isAlive() + Effect.RESET);
        Reposilite.getLogger().info("  Uptime: " + TimeUtils.format(reposilite.getUptime() / 1000.0 / 60.0) + "min");
        Reposilite.getLogger().info("  Memory usage of process: " + getMemoryUsage());
        Reposilite.getLogger().info("  Cached elements: " + reposilite.getMetadataService().getCacheSize());
        printExceptions(reposilite.getHttpServer().getExceptions());
        Reposilite.getLogger().info("");
        return true;
    }

    private void printExceptions(Collection<Exception> exceptions) {
        if (exceptions.isEmpty()) {
            return;
        }

        Reposilite.getLogger().info("List of cached exceptions:");
        int count = 0;

        for (Exception throwable : exceptions) {
            Reposilite.getLogger().error("Exception " + (++count), throwable);
        }
    }

    private String getMemoryUsage() {
        return TimeUtils.format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0D / 1024.0D) + "M";
    }

}
