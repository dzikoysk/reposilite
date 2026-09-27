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

import com.reposilite.repository.api.RepositoryConfiguration
import com.reposilite.status.FailureFacade
import com.reposilite.storage.s3.S3StorageProviderSettings
import com.reposilite.storage.s3.findS3SharedBucketConflicts
import panda.std.reactive.Reference
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

internal class RepositoryCollections(private val failureFacade: FailureFacade) {

    private class Registration(
        val settings: () -> List<RepositoryConfiguration>,
        val shutdown: () -> Boolean,
        val load: () -> Unit,
    )

    private val registrations = mutableListOf<Registration>()

    fun <S : RepositoryConfiguration, R> register(
        source: Reference<List<S>>,
        create: (List<S>) -> Map<String, R>,
        shutdown: (R) -> Unit,
    ): Supplier<Map<String, R>> {
        val repositories = AtomicReference<Map<String, R>>(emptyMap())
        val pendingShutdown = mutableListOf<R>()
        registrations += Registration(
            settings = { source.get() },
            shutdown = {
                pendingShutdown += repositories.getAndSet(emptyMap()).values
                pendingShutdown.removeAll { repository ->
                    runCatching { shutdown(repository) }
                        .onFailure { failureFacade.throwException("Cannot stop repository", it) }
                        .isSuccess
                }
                pendingShutdown.isEmpty()
            },
            load = {
                runCatching { create(source.get()) }
                    .onSuccess { repositories.set(it) }
                    .onFailure { failureFacade.throwException("Cannot load repositories", it) }
            },
        )
        source.subscribe { reload() }
        reload()
        return Supplier { repositories.get() }
    }

    fun shutdown(): Boolean =
        registrations.map { it.shutdown() }.all { it }

    private fun reload() {
        if (!shutdown()) {
            return
        }

        val storage = registrations.flatMap { it.settings() }
            .mapNotNull { repository ->
                (repository.storageProvider as? S3StorageProviderSettings)?.let { repository.id to it }
            }

        val conflicts = findS3SharedBucketConflicts(storage)
        if (conflicts.isNotEmpty()) {
            failureFacade.throwException("Cannot load repositories", IllegalArgumentException("Overlapping S3 storage: ${conflicts.joinToString()}"))
            return
        }

        registrations.forEach { it.load() }
    }

}
