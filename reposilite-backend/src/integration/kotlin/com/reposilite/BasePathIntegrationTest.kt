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

package com.reposilite

import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.configuration.shared.SharedConfigurationFacade
import io.javalin.Javalin
import io.javalin.http.Context
import kong.unirest.HttpRequest
import kong.unirest.Unirest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CountDownLatch

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class BasePathIntegrationTest : ReposiliteSpecification() {

    private val basePath = "/custom-base-path"

    override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
        sharedConfigurationFacade.getDomainSettings<FrontendSettings>().update {
            it.copy(basePath = basePath)
        }
    }

    @Disabled
    @Test
    fun `run reposilite with custom base path`() {
        val await = CountDownLatch(1)

        Javalin.create()
            .get("/") { it.html("Index") }
            .get(basePath) { Unirest.get(it.reposiliteLocation()).redirect(it) }
            .get("$basePath/<uri>") { Unirest.get(it.reposiliteLocation()).redirect(it) }
            .head("$basePath/<uri>") { Unirest.head(it.reposiliteLocation()).redirect(it) }
            .post("$basePath/<uri>") { Unirest.post(it.reposiliteLocation()).redirect(it) }
            .put("$basePath/<uri>") { Unirest.put(it.reposiliteLocation()).redirect(it) }
            .delete("$basePath/<uri>") { Unirest.delete(it.reposiliteLocation()).redirect(it) }
            .options("$basePath/<uri>") { Unirest.options(it.reposiliteLocation()).redirect(it) }
            .get("/stop") { await.countDown() }
            .start(80)

        await.await()
    }

    private fun <R : HttpRequest<*>> R.redirect(ctx: Context) {
        ctx.headerMap().forEach { (key, value) -> header(key, value) }
        val response = this.asBytes()
        response.headers.all().forEach { ctx.header(it.name, it.value) }
        ctx.status(response.status).result(response.body)
    }

    private fun Context.reposiliteLocation(): String =
        "http://localhost:${reposilite.parameters.port}/${pathParamMap()["uri"] ?: ""}"

}
