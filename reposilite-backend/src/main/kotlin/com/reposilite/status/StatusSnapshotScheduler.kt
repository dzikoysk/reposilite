package com.reposilite.status

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

internal class StatusSnapshotScheduler(private val scheduler: ScheduledExecutorService, private val statusFacade: StatusFacade) {

    private var currentTask: ScheduledFuture<*>? = null

    fun start() {
        scheduleStatusSnapshot(MINUTES)
    }

    private fun scheduleStatusSnapshot(timeUnit: TimeUnit) {
        currentTask = scheduler.schedule({
            statusFacade.recordStatusSnapshot()

            when {
                statusFacade.getLatestStatusSnapshots().size >= 12 -> scheduleStatusSnapshot(HOURS)
                else -> scheduleStatusSnapshot(MINUTES)
            }
        }, 1, timeUnit)
    }

    fun stop() {
        runCatching {
            currentTask?.cancel(true)
        }
    }

}