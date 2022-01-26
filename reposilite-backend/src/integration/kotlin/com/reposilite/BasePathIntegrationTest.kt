package com.reposilite

import com.reposilite.settings.api.SharedConfiguration
import io.javalin.Javalin
import io.javalin.http.Context
import kong.unirest.HttpRequest
import kong.unirest.Unirest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CountDownLatch

@ExtendWith(ReposiliteLocalIntegrationJunitExtension::class)
internal class BasePathIntegrationTest : ReposiliteSpecification() {

    private val basePath = "/custom-base-path"

    override fun overrideSharedConfiguration(sharedConfiguration: SharedConfiguration) {
        sharedConfiguration.basePath.update(basePath)
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