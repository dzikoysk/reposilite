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

package org.panda_lang.reposilite.maven

import net.dzikoysk.dynamiclogger.Journalist
import org.panda_lang.reposilite.config.Configuration.RepositoryConfiguration
import org.panda_lang.reposilite.storage.StorageProviderFactory

internal class RepositoryServiceFactory(private val journalist: Journalist) {

    fun createRepositoryService(repositoriesConfigurations: Map<String, RepositoryConfiguration>): RepositoryService {
        val storageProviderFactory = StorageProviderFactory()
        val repositoryFactory = RepositoryFactory(storageProviderFactory)
        val repositories: MutableMap<String, Repository> = LinkedHashMap(repositoriesConfigurations.size)

        for ((repositoryName, repositoryConfiguration) in repositoriesConfigurations) {
            repositories[repositoryName] = repositoryFactory.createRepository(repositoryName, repositoryConfiguration)
        }

        return RepositoryService(journalist, repositories)
    }

}