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
import com.reposilite.maven.MavenFacade.Companion.REPOSITORIES
import com.reposilite.maven.Repository
import com.reposilite.storage.StorageProviderFactory.createStorageProvider
import net.dzikoysk.dynamiclogger.Journalist
import java.nio.file.Path

internal object RepositoryFactory {

    fun createRepository(journalist: Journalist, workingDirectory: Path, repositoryName: String, repositoryConfiguration: RepositoryConfiguration): Repository =
        Repository(
            repositoryName,
            repositoryConfiguration.visibility,
            createStorageProvider(
                journalist,
                workingDirectory.resolve(REPOSITORIES).resolve(repositoryName),
                repositoryConfiguration.storageProvider,
                repositoryConfiguration.diskQuota
            ),
            repositoryConfiguration.redeployment
        )

}