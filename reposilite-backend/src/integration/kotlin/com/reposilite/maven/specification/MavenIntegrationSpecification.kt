/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.maven.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.maven.application.MavenSettings
import io.javalin.Javalin
import io.javalin.config.JavalinConfig

internal abstract class MavenIntegrationSpecification : ReposiliteSpecification() {

    protected fun useProxiedHost(
        repository: String,
        gav: String,
        content: String,
        block: (String, String) -> Unit
    ) {
        val application = startProxiedHost { config ->
            config.routes.head("/$repository/$gav") { ctx -> ctx.result(content) }
            config.routes.get("/$repository/$gav") { ctx -> ctx.result(content) }
        }

        try {
            block(gav, content)
        } finally {
            application.stop()
        }
    }

    protected fun startProxiedHost(configure: (JavalinConfig) -> Unit): Javalin {
        val application = Javalin.create { config -> configure(config) }.start(0)

        try {
            val reference = "http://localhost:${application.port()}/releases"
            useFacade<SharedConfigurationFacade>()
                .getDomainSettings<MavenSettings>()
                .update { settings ->
                    settings.copy(
                        repositories = settings.repositories.map { repository ->
                            if (repository.id == "proxied" || repository.id == "proxied-stored") {
                                repository.copy(
                                    proxied = repository.proxied.map { mirror -> mirror.copy(reference = reference) }
                                )
                            } else {
                                repository
                            }
                        }
                    )
                }
        } catch (exception: Exception) {
            application.stop()
            throw exception
        }

        return application
    }

}
