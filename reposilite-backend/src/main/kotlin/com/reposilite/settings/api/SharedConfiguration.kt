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
import com.reposilite.maven.RepositoryVisibility
import com.reposilite.maven.application.RepositoriesSettings
import com.reposilite.shared.extensions.Validator
import com.reposilite.statistics.application.StatisticsSettings
import net.dzikoysk.cdn.entity.Contextual
import net.dzikoysk.cdn.entity.Description
import net.dzikoysk.cdn.serdes.DeserializationHandler
import panda.std.reactive.mutableReference
import panda.utilities.StringUtils
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.Serializable

class SharedConfiguration : DeserializationHandler<SharedConfiguration> {

    val repositories = mutableReference(RepositoriesSettings())

    val advanced = mutableReference(AdvancedSettings())

    val appearance = mutableReference(AppearanceSettings())

    val statistics = mutableReference(StatisticsSettings())

    val ldap = mutableReference(LdapSettings())

    override fun handle(sharedConfiguration: SharedConfiguration): SharedConfiguration {
        var formattedBasePath = advanced.get().basePath

        // verify base path
        if (!StringUtils.isEmpty(formattedBasePath)) {
            if (!formattedBasePath.startsWith("/")) {
                formattedBasePath = "/$formattedBasePath"
            }

            if (!formattedBasePath.endsWith("/")) {
                formattedBasePath += "/"
            }

            this.advanced.update {
                AdvancedSettings(
                    formattedBasePath,
                    it.frontend,
                    it.swagger,
                    it.forwardedIp,
                    it.icpLicense
                )
            }
        }

        return this
    }
}
