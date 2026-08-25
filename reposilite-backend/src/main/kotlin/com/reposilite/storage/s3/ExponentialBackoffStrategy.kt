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

package com.reposilite.storage.s3

import java.time.Duration
import kotlin.math.min

import software.amazon.awssdk.core.retry.RetryPolicyContext
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy

internal class ExponentialBackoffStrategy(
    private val baseDelay: Duration,
    private val maxBackoff: Duration
) : BackoffStrategy {

    override fun computeDelayBeforeNextRetry(context: RetryPolicyContext): Duration {
        val retriesAttempted = context.retriesAttempted().toLong()

        val delayMillis: Long = min(
            baseDelay.toMillis() * (1L shl retriesAttempted.toInt()),
            maxBackoff.toMillis()
        )

        return Duration.ofMillis(delayMillis)
    }

}