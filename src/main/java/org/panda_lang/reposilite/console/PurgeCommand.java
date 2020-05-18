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
import org.panda_lang.reposilite.metadata.MetadataService;

final class PurgeCommand implements NanoCommand {

    @Override
    public boolean call(Reposilite reposilite) {
        MetadataService metadataService = reposilite.getMetadataService();
        int cacheSize = metadataService.getCacheSize();

        reposilite.getMetadataService().purgeCache();
        Reposilite.getLogger().info("Purged " + cacheSize + " elements");

        return true;
    }

}
