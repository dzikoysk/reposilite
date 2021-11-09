package com.reposilite.token.infrastructure

import com.reposilite.ReposiliteLocalIntegrationJunitExtension
import com.reposilite.token.AccessTokenIntegrationTest
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ReposiliteLocalIntegrationJunitExtension::class)
internal class LocalAccessTokenIntegrationTest : AccessTokenIntegrationTest()