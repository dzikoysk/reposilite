package com.reposilite.maven.infrastructure

import com.reposilite.ReposiliteRemoteIntegrationJunitExtension
import com.reposilite.maven.MavenApiIntegrationTest
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ReposiliteRemoteIntegrationJunitExtension::class)
internal class RemoteMavenApiIntegrationTest : MavenApiIntegrationTest()