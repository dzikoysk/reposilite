package com.reposilite.statistics

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

fun createDateIntervalProvider(mode: String): DateIntervalProvider =
    when (mode) {
        "daily" -> DailyDateIntervalProvider
        "weekly" -> WeeklyDateIntervalProvider
        "monthly" -> MonthlyDateIntervalProvider
        "yearly" -> YearlyDateIntervalProvider
        else -> throw IllegalStateException("Unsupported date interval provided: $mode")
    }

sealed interface DateIntervalProvider {

    fun createDate(): LocalDate

}

internal object DailyDateIntervalProvider : DateIntervalProvider {

    override fun createDate(): LocalDate =
        LocalDate.now()

}

internal object WeeklyDateIntervalProvider : DateIntervalProvider {

    private val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    override fun createDate(): LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

}

internal object MonthlyDateIntervalProvider : DateIntervalProvider {

    override fun createDate(): LocalDate =
        LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())

}

internal object YearlyDateIntervalProvider : DateIntervalProvider {

    override fun createDate(): LocalDate =
        LocalDate.now().with(TemporalAdjusters.firstDayOfYear())

}