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

import org.panda_lang.nanomaven.NanoConstants;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.utilities.commons.console.Effect;

final class StatusCommand implements NanoCommand {

    @Override
    public boolean call(NanoMaven nanoMaven) {
        NanoMaven.getLogger().info("");
        NanoMaven.getLogger().info("NanoMaven " + NanoConstants.VERSION + " Status");
        NanoMaven.getLogger().info("  Active: " + Effect.GREEN_BOLD + nanoMaven.getHttpServer().isAlive() + Effect.RESET);
        NanoMaven.getLogger().info("  Uptime: " + format(nanoMaven.getUptime() / 1000.0 / 60.0) + "min");
        NanoMaven.getLogger().info("  Memory usage of process: " + format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0D / 1024.0D) + "M");
        NanoMaven.getLogger().info("  Cached elements: " + nanoMaven.getMetadataService().getCacheSize());
        nanoMaven.getHttpServer().getLatestError().peek(throwable -> NanoMaven.getLogger().error(" Latest exception", throwable));
        NanoMaven.getLogger().info("");

        return true;
    }

    private String format(double number) {
        return String.format("%.2f", number);
    }

}
