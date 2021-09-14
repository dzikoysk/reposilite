package com.reposilite

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
internal class ReposiliteRemoteIntegrationJunitExtension : Extension, BeforeEachCallback, AfterEachCallback {

    private class SpecifiedMariaDBContainer(image: String) : MariaDBContainer<SpecifiedMariaDBContainer>(DockerImageName.parse(image))

    @Container
    private val mariaDb = SpecifiedMariaDBContainer("mariadb:10.6.1")

    @Container
    private val localstack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.17"))
        .withServices(S3)

    override fun beforeEach(context: ExtensionContext?) {
        mariaDb.start()
        localstack.start()

        context?.also {
            val instance = it.requiredTestInstance
            val type = instance::class.java

            type.getField("_extensionInitialized").set(instance, true)
            type.getField("_database").set(instance, "mysql ${mariaDb.host}:${mariaDb.getMappedPort(3306)} ${mariaDb.databaseName} ${mariaDb.username} ${mariaDb.password}")
            type.getField("_storageProvider").set(instance, "s3 -e ${localstack.getEndpointOverride(S3)} ${localstack.accessKey} ${localstack.secretKey} ${localstack.region} {repository}")
        }
    }

    override fun afterEach(context: ExtensionContext?) {
        mariaDb.stop()
        localstack.stop()
    }

}