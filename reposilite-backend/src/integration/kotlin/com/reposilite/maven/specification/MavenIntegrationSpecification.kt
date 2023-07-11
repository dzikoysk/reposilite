/*
 * Copyright (c) 2023 dzikoysk
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
import io.javalin.Javalin
import kotlinx.coroutines.Job

internal abstract class MavenIntegrationSpecification : ReposiliteSpecification() {

    protected suspend fun useProxiedHost(
        repository: String,
        gav: String,
        content: String,
        block: (String, String) -> Unit
    ) {
        val serverStartedJob = Job()

        val application = Javalin.create()
            .events { it.serverStarted { serverStartedJob.complete() } }
            .head("/$repository/$gav") { ctx -> ctx.result(content) }
            .get("/$repository/$gav") { ctx -> ctx.result(content) }
            .start(reposilite.parameters.port + 1)

        serverStartedJob.join()
        block(gav, content)
        application.stop()
    }

}