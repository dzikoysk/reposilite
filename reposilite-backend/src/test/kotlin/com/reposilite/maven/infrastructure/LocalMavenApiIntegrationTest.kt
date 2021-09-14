package com.reposilite.maven.infrastructure

import com.reposilite.ReposiliteLocalIntegrationJunitExtension
import com.reposilite.maven.MavenApiIntegrationTest
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ReposiliteLocalIntegrationJunitExtension::class)
internal class LocalMavenApiIntegrationTest : MavenApiIntegrationTest()