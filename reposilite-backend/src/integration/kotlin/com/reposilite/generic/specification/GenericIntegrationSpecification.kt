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

package com.reposilite.generic.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.generic.GenericFacade
import com.reposilite.generic.application.GenericRepositorySettings
import com.reposilite.generic.application.GenericSettings
import kong.unirest.core.Unirest.put
import org.assertj.core.api.Assertions.assertThat

internal abstract class GenericIntegrationSpecification : ReposiliteSpecification() {

    protected val genericFacade by lazy { useFacade<GenericFacade>() }
    protected val genericSettings by lazy {
        useFacade<SharedConfigurationFacade>().getDomainSettings<GenericSettings>()
    }

    protected abstract fun repositories(): List<GenericRepositorySettings>

    override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
        sharedConfigurationFacade.getDomainSettings<GenericSettings>().update {
            GenericSettings(repositories = repositories())
        }
    }

    protected fun useRepositories(vararg repositories: GenericRepositorySettings) {
        genericSettings.update { settings ->
            settings.copy(repositories = settings.repositories + repositories)
        }
    }

    protected fun useGenericFile(repository: String, path: String, content: String): String {
        val address = "$base/$repository/$path"
        val (name, secret) = useDefaultManagementToken()
        val response = put(address).basicAuth(name, secret).body(content).asEmpty()
        assertThat(response.isSuccess).isTrue
        return address
    }

}
