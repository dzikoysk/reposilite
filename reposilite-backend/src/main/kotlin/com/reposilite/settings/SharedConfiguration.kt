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

package com.reposilite.settings

import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.maven.application.RepositoriesSettings
import com.reposilite.settings.api.Settings
import com.reposilite.statistics.application.StatisticsSettings
import com.reposilite.web.application.WebSettings
import net.dzikoysk.cdn.entity.Description
import net.dzikoysk.cdn.serdes.DeserializationHandler
import panda.std.reactive.mutableReference
import panda.utilities.StringUtils

const val SHARED_CONFIGURATION_FILE = "configuration.shared.json"

class SharedConfiguration : DeserializationHandler<SharedConfiguration> {

    @Description("# Web domain configuration")
    val web = mutableReference(WebSettings())

    @Description("# Repository domain configuration")
    val repositories = mutableReference(RepositoriesSettings())

    @Description("# Frontend domain configuration")
    val frontend = mutableReference(FrontendSettings())

    @Description("# Statistics domain configuration")
    val statistics = mutableReference(StatisticsSettings())

    @Description("# Authentication domain configuration")
    val authentication = mutableReference(AuthenticationSettings())

    override fun handle(sharedConfiguration: SharedConfiguration): SharedConfiguration {
        var formattedBasePath = frontend.map { it.basePath }

        // verify base path
        if (!StringUtils.isEmpty(formattedBasePath)) {
            if (!formattedBasePath.startsWith("/")) {
                formattedBasePath = "/$formattedBasePath"
            }

            if (!formattedBasePath.endsWith("/")) {
                formattedBasePath += "/"
            }

            this.frontend.update {
                it.copy(basePath = formattedBasePath)
            }
        }

        return this
    }

    fun update(settings: Settings): Settings =
        settings
            .let {
                repositories.update(RepositoriesSettings(it.repositories))
                web.update(it.web)
                frontend.update(it.frontend)
                statistics.update(it.statistics)
                authentication.update(it.authentication)
                this
            }
            .toDto()

    fun toDto(): Settings =
        Settings(
            frontend = frontend.get(),
            web = web.get(),
            repositories = repositories.get().repositories,
            statistics = statistics.get(),
            authentication = authentication.get()
        )
}
