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

package com.reposilite.storage.s3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.SdkHttpService
import software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sts.StsClient
import java.net.URI
import java.util.ServiceLoader

internal class AwsHttpClientTest {

    private val credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
    private val endpoint = URI.create("http://127.0.0.1:9")

    @Test
    fun `should use URLConnection for S3 client`() {
        val httpServices = ServiceLoader.load(SdkHttpService::class.java).toList()
        assertThat(httpServices.map { it.javaClass.name })
            .containsExactly(UrlConnectionSdkHttpService::class.java.name)

        val s3 = S3Client.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(endpoint)
            .credentialsProvider(credentials)
            .build()

        try {
            assertThat(s3).isNotNull
        } finally {
            s3.close()
        }
    }

    @Test
    fun `should use URLConnection for STS client`() {
        val httpServices = ServiceLoader.load(SdkHttpService::class.java).toList()
        assertThat(httpServices.map { it.javaClass.name })
            .containsExactly(UrlConnectionSdkHttpService::class.java.name)

        val sts = StsClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(endpoint)
            .credentialsProvider(credentials)
            .build()

        try {
            assertThat(sts).isNotNull
        } finally {
            sts.close()
        }
    }

}
