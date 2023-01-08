/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.configuration.local

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.parameters

@Plugin(name = "local-configuration", dependencies = ["configuration"])
class LocalConfigurationPlugin : ReposilitePlugin() {

    override fun initialize(): LocalConfiguration {
        logger.info("")
        logger.info("--- Local configuration")

        return LocalConfigurationFactory.createLocalConfiguration(this, parameters())
    }

}
