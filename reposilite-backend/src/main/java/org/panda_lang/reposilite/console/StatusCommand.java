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
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.TimeUtils;
import org.panda_lang.utilities.commons.IOUtils;
import org.panda_lang.utilities.commons.console.Effect;
import picocli.CommandLine.Command;

import java.util.List;

@Command(name = "status", description = "Display summary status of app health")
final class StatusCommand implements ReposiliteCommand {

    private final Reposilite reposilite;

    StatusCommand(Reposilite reposilite) {
        this.reposilite = reposilite;
    }

    @Override
    public boolean execute(List<String> response) {
        String latestVersion = reposilite.isTestEnvEnabled() ? ReposiliteConstants.VERSION : getVersion();

        response.add("Reposilite " + ReposiliteConstants.VERSION + " Status");
        response.add("  Active: " + Effect.GREEN_BOLD + reposilite.getHttpServer().isAlive() + Effect.RESET);
        response.add("  Uptime: " + TimeUtils.format(reposilite.getUptime() / 1000.0 / 60.0) + "min");
        response.add("  Memory usage of process: " + getMemoryUsage());
        response.add("  Disk usage: " + FilesUtils.humanReadableByteCount(reposilite.getStorageProvider().getUsage()));
        response.add("  Cached metadata: " + reposilite.getMetadataService().getCacheSize());
        response.add("  Exceptions: " + reposilite.getFailureService().getExceptions().size());
        response.add("  Latest version of reposilite: " + latestVersion);

        return true;
    }

    private String getVersion() {
        String latest = IOUtils
                .fetchContent(ReposiliteConstants.REMOTE_VERSION)
                .orElseGet(ioException -> ReposiliteConstants.REMOTE_VERSION + " is unavailable: " + ioException.getMessage());

        return (ReposiliteConstants.VERSION.equals(latest) ? Effect.GREEN : Effect.RED_UNDERLINED) + latest + Effect.RESET;
    }

    private String getMemoryUsage() {
        return TimeUtils.format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0D / 1024.0D) + "M";
    }

}
