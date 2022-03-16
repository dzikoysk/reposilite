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

package com.reposilite.settings.api

import com.reposilite.auth.application.LdapSettings
import com.reposilite.frontend.application.AppearanceSettings
import com.reposilite.maven.application.RepositoriesSettings
import com.reposilite.statistics.application.StatisticsSettings
import com.reposilite.web.application.WebSettings
import net.dzikoysk.cdn.serdes.DeserializationHandler
import panda.std.reactive.mutableReference
import panda.utilities.StringUtils

class SharedConfiguration : DeserializationHandler<SharedConfiguration> {

    val web = mutableReference(WebSettings())

    val repositories = mutableReference(RepositoriesSettings())

    val appearance = mutableReference(AppearanceSettings())

    val statistics = mutableReference(StatisticsSettings())

    val ldap = mutableReference(LdapSettings())

    override fun handle(sharedConfiguration: SharedConfiguration): SharedConfiguration {
        var formattedBasePath = appearance.map { it.basePath }

        // verify base path
        if (!StringUtils.isEmpty(formattedBasePath)) {
            if (!formattedBasePath.startsWith("/")) {
                formattedBasePath = "/$formattedBasePath"
            }

            if (!formattedBasePath.endsWith("/")) {
                formattedBasePath += "/"
            }

            this.appearance.update {
                it.copy(basePath = formattedBasePath)
            }
        }

        return this
    }

}
