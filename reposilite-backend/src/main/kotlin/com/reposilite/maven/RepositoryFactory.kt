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

import com.reposilite.journalist.Journalist
import com.reposilite.settings.SharedConfiguration.RepositoryConfiguration
import com.reposilite.settings.SharedConfiguration.RepositoryConfiguration.ProxiedHostConfiguration
import com.reposilite.shared.RemoteClientProvider
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import com.reposilite.storage.StorageProviderFactory.createStorageProvider
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Proxy.Type.HTTP
import java.nio.file.Path
import java.nio.file.Paths

internal class RepositoryFactory(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val remoteClientProvider: RemoteClientProvider
) {

    private val repositories = Paths.get("repositories")

    fun createRepository(repositoryName: String, configuration: RepositoryConfiguration): Repository =
        Repository(
            repositoryName,
            configuration.visibility,
            configuration.redeployment,
            configuration.proxied.map { createProxiedHostConfiguration(it) },
            createStorageProvider(journalist, workingDirectory.resolve(repositories), repositoryName, configuration.storageProvider),
        )

    private fun createProxiedHostConfiguration(configurationSource: String): ProxiedHost {
        val (name, configuration) = loadCommandBasedConfiguration(ProxiedHostConfiguration(), configurationSource)

        val host =
            if (name.endsWith("/"))
                name.substring(0, name.length - 1)
            else
                name

        val proxy = configuration.proxy
            .takeIf { it.isNotEmpty() }
            ?.let { Proxy(HTTP, InetSocketAddress(it.substringBeforeLast(":"), it.substringAfterLast(":").toInt())) }

        return ProxiedHost(
            host,
            configuration,
            remoteClientProvider.createClient(journalist, proxy)
        )
    }

}