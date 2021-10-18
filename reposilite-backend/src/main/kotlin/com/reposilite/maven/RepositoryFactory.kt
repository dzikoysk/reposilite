/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.maven

import com.reposilite.config.Configuration.RepositoryConfiguration
import com.reposilite.config.Configuration.RepositoryConfiguration.ProxiedHostConfiguration
import com.reposilite.journalist.Journalist
import com.reposilite.shared.loadCommandBasedConfiguration
import com.reposilite.storage.StorageProviderFactory.createStorageProvider
import java.nio.file.Path
import java.nio.file.Paths

internal class RepositoryFactory(
    private val journalist: Journalist,
    private val workingDirectory: Path,
) {

    private val repositories = Paths.get("repositories")

    fun createRepository(repositoryName: String, repositoryConfiguration: RepositoryConfiguration): Repository =
        Repository(
            repositoryName,
            repositoryConfiguration.visibility,
            repositoryConfiguration.proxied.associate { createProxiedHostConfiguration(it) },
            createStorageProvider(journalist, workingDirectory.resolve(repositories), repositoryName, repositoryConfiguration.storageProvider),
            repositoryConfiguration.redeployment
        )

    private fun createProxiedHostConfiguration(configurationSource: String): Pair<String, ProxiedHostConfiguration> =
        with(loadCommandBasedConfiguration(ProxiedHostConfiguration(), configurationSource)) {
            if (name.endsWith("/"))
                Pair(name.substring(0, name.length - 1), configuration)
            else
                Pair(name, configuration)
        }

}