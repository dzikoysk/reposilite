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

package com.reposilite.configuration.application

import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.reposilite
import com.mongodb.client.MongoClients

@Plugin(name = "configuration")
class ConfigurationPlugin : ReposilitePlugin() {

    override fun initialize(): ConfigurationFacade {
        val parameters = reposilite().parameters
        val databaseConnection = reposilite().databaseConnection
        val mongoClient = if (parameters.database.startsWith("mongodb")) MongoClients.create(parameters.database) else null
        val databaseName = if (parameters.database.startsWith("mongodb")) parameters.database.split("/").last() else null

        return ConfigurationComponents(databaseConnection.database, mongoClient, databaseName).configurationFacade()
    }

}
