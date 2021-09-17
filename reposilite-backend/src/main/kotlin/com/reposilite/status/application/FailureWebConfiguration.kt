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

package com.reposilite.status.application

import com.reposilite.Reposilite
import com.reposilite.journalist.Journalist
import com.reposilite.status.FailureFacade
import com.reposilite.status.FailuresCommand
import com.reposilite.status.infrastructure.FailureHandler
import com.reposilite.web.WebConfiguration
import io.javalin.Javalin

internal object FailureWebConfiguration : WebConfiguration {

    fun createFacade(journalist: Journalist) =
        FailureFacade(journalist)

    override fun initialize(reposilite: Reposilite) {
        reposilite.consoleFacade.registerCommand(FailuresCommand(reposilite.failureFacade))
    }

    override fun javalin(reposilite: Reposilite, javalin: Javalin) {
        javalin.exception(Exception::class.java, FailureHandler(reposilite.failureFacade))
    }

}