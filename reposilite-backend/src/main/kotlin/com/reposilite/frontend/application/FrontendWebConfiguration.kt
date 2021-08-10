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

package com.reposilite.frontend.application

import com.reposilite.frontend.FrontendFacade
import com.reposilite.frontend.infrastructure.CustomFrontendHandler
import com.reposilite.frontend.infrastructure.ResourcesFrontendHandler
import com.reposilite.web.ReposiliteRoutes
import java.nio.file.Path
import kotlin.io.path.exists

internal object FrontendWebConfiguration {

    private const val CUSTOM_FRONTEND_DIRECTORY = "frontend"

    fun createFacade(): FrontendFacade =
        FrontendFacade()

    fun routing(frontendFacade: FrontendFacade, workingDirectory: Path): Set<ReposiliteRoutes> =
        setOf(
            workingDirectory.resolve(CUSTOM_FRONTEND_DIRECTORY)
                .takeIf { it.exists() }
                ?.let { CustomFrontendHandler(frontendFacade, workingDirectory.resolve(CUSTOM_FRONTEND_DIRECTORY)) }
                ?: ResourcesFrontendHandler(frontendFacade)
        )

}