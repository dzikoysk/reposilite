package com.reposilite.token.infrastructure

import com.reposilite.ReposiliteRemoteIntegrationJunitExtension
import com.reposilite.token.AccessTokenIntegrationTest
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ReposiliteRemoteIntegrationJunitExtension::class)
internal class RemoteAccessTokenIntegrationTest : AccessTokenIntegrationTest()