/*
 * Copyright (c) 2022 dzikoysk
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

import com.reposilite.maven.application.ProxiedRepository
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Proxy.Type.HTTP
import java.nio.file.Path
import java.nio.file.Paths

internal class RepositoryFactory(
    private val workingDirectory: Path,
    private val remoteClientProvider: RemoteClientProvider,
    private val repositoryProvider: RepositoryProvider,
    private val failureFacade: FailureFacade,
    private val storageFacade: StorageFacade,
    private val repositoriesNames: Collection<String>,
) {

    private val repositoriesDirectory = Paths.get("repositories")

    fun createRepository(repositoryName: String, configuration: RepositorySettings): Repository =
        Repository(
            repositoryName,
            configuration.visibility,
            configuration.redeployment,
            configuration.preserveSnapshots,
            configuration.proxied.map { createProxiedHostConfiguration(it) },
            storageFacade.createStorageProvider(failureFacade, workingDirectory.resolve(repositoriesDirectory), repositoryName, configuration.storageProvider),
        )

    private fun createProxiedHostConfiguration(configurationSource: ProxiedRepository): ProxiedHost {
        val name = configurationSource.reference
        val configuration = configurationSource

        val host =
            if (name.endsWith("/"))
                name.substring(0, name.length - 1)
            else
                name

        val remoteClient =
            if (repositoriesNames.contains(host))
                RepositoryLoopbackClient(lazy { repositoryProvider.getRepositories()[host]!! })
            else
                configuration.proxy
                    .takeIf { it.isNotEmpty() }
                    ?.let { Proxy(HTTP, InetSocketAddress(it.substringBeforeLast(":"), it.substringAfterLast(":").toInt())) }
                    .let { remoteClientProvider.createClient(failureFacade, it) }

        return ProxiedHost(host, configuration, remoteClient)
    }

}
