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