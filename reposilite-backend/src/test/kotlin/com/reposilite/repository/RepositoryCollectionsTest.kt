/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.repository

import com.reposilite.repository.specification.RepositoryCollectionsSpecification
import com.reposilite.storage.s3.S3StorageProviderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.reactive.mutableReference

internal class RepositoryCollectionsTest : RepositoryCollectionsSpecification() {

    @Test
    fun `should leave conflicting repositories offline during initialization`() {
        // given: saved settings containing overlapping S3 locations
        val storage = S3StorageProviderSettings(bucketName = "shared")
        val settings = mutableReference(listOf(TestSettings("first", storage), TestSettings("second", storage)))

        // when: repositories are registered during startup
        val repositories = useRepositories(settings)

        // then: the conflict is reported without creating repositories or discarding settings
        assertThat(repositories.get()).isEmpty()
        assertThat(operations).isEmpty()
        assertThat(settings.get()).hasSize(2)
        assertThat(failures.hasFailures()).isTrue()
    }

    @Test
    fun `should stop every repository before starting replacements`() {
        // given: two independently configured repository collections
        val firstSettings = mutableReference(listOf(TestSettings("first")))
        val first = useRepositories(firstSettings)
        val second = useRepositories(mutableReference(listOf(TestSettings("second"))))
        val previousFirst = first.get().getValue("first")
        val previousSecond = second.get().getValue("second")
        operations.clear()

        // when: one collection's settings change
        firstSettings.update { listOf(TestSettings("renamed")) }

        // then: both old repositories close before either replacement starts
        assertThat(operations).containsExactly("stop", "stop", "start:renamed", "start:second")
        assertThat(previousFirst.closed).isTrue()
        assertThat(previousSecond.closed).isTrue()
        assertThat(first.get()).containsOnlyKeys("renamed")
        assertThat(second.get().getValue("second")).isNotSameAs(previousSecond)
    }

    @Test
    fun `should preserve conflicting settings and recover after a correction`() {
        // given: distinct configured storage paths
        val storage = S3StorageProviderSettings(bucketName = "shared", sharedBucket = true)
        val firstSettings = mutableReference(listOf(TestSettings("first", storage)))
        val secondSettings = mutableReference(listOf(TestSettings("second", storage)))
        val first = useRepositories(firstSettings)
        val second = useRepositories(secondSettings)
        val invalid = listOf(TestSettings("second", storage.copy(prefix = "first", sharedBucket = false)))

        // when: an update introduces overlapping storage
        secondSettings.update(invalid)

        // then: settings are retained and no repositories are loaded
        assertThat(secondSettings.get()).isEqualTo(invalid)
        assertThat(first.get()).isEmpty()
        assertThat(second.get()).isEmpty()
        assertThat(failures.hasFailures()).isTrue()

        // when: the conflicting configuration is corrected
        secondSettings.update(listOf(TestSettings("second", storage)))

        // then: both collections are loaded again
        assertThat(first.get()).containsOnlyKeys("first")
        assertThat(second.get()).containsOnlyKeys("second")
    }

    @Test
    fun `should recover when a storage transfer updates its destination first`() {
        // given: one repository using an S3 location
        val storage = S3StorageProviderSettings(bucketName = "shared")
        val sourceSettings = mutableReference(listOf(TestSettings("source", storage)))
        val destinationSettings = mutableReference(emptyList<TestSettings>())
        val source = useRepositories(sourceSettings)
        val destination = useRepositories(destinationSettings)
        operations.clear()

        // when: a remote snapshot assigns storage before removing its previous owner
        destinationSettings.update(listOf(TestSettings("destination", storage)))
        sourceSettings.update(emptyList<TestSettings>())

        // then: the destination starts after the old owner stops, without another update
        assertThat(operations).containsExactly("stop", "start:destination")
        assertThat(source.get()).isEmpty()
        assertThat(destination.get()).containsOnlyKeys("destination")
    }

    @Test
    fun `should continue closing repositories when one shutdown fails`() {
        // given: two repositories, one of which fails to close
        val settings = mutableReference(listOf(TestSettings("first"), TestSettings("second")))
        val repositories = useRepositories(settings)
        val first = repositories.get().getValue("first")
        first.failShutdown = true
        val second = repositories.get().getValue("second")
        operations.clear()

        // when: their settings are updated
        settings.update(listOf(TestSettings("replacement")))

        // then: the new settings remain available without starting replacements
        assertThat(settings.get()).containsExactly(TestSettings("replacement"))
        assertThat(operations).containsExactly("stop", "stop")
        assertThat(second.closed).isTrue()
        assertThat(repositories.get()).isEmpty()
        assertThat(failures.hasFailures()).isTrue()

        // when: another update arrives while closing the first repository still fails
        operations.clear()
        settings.update { it }

        // then: the failed close is retried and replacements remain offline
        assertThat(operations).containsExactly("stop")
        assertThat(repositories.get()).isEmpty()

        // when: the first repository can be closed on the next update
        first.failShutdown = false
        operations.clear()
        settings.update { it }

        // then: the old instance closes before a replacement starts
        assertThat(operations).containsExactly("stop", "start:replacement")
        assertThat(first.closed).isTrue()
        assertThat(repositories.get()).containsOnlyKeys("replacement")
    }
}
