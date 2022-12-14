/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.statistics

import com.reposilite.statistics.api.ResolvedRequestsInterval
import com.reposilite.statistics.api.ResolvedRequestsInterval.DAILY
import com.reposilite.statistics.api.ResolvedRequestsInterval.MONTHLY
import com.reposilite.statistics.api.ResolvedRequestsInterval.WEEKLY
import com.reposilite.statistics.api.ResolvedRequestsInterval.YEARLY
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

fun createDateIntervalProvider(mode: ResolvedRequestsInterval): DateIntervalProvider = when (mode) {
    DAILY -> DailyDateIntervalProvider
    WEEKLY -> WeeklyDateIntervalProvider
    MONTHLY -> MonthlyDateIntervalProvider
    YEARLY -> YearlyDateIntervalProvider
}

sealed interface DateIntervalProvider {

    fun createDate(): LocalDate

    fun createTimeSeries(): List<LocalDate>

}

internal object DailyDateIntervalProvider : DateIntervalProvider {

    override fun createDate(): LocalDate =
        LocalDate.now()

    override fun createTimeSeries(): List<LocalDate> =
        (0..364).map { createDate().minusDays(it.toLong()) }

}

internal object WeeklyDateIntervalProvider : DateIntervalProvider {

    private val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    override fun createDate(): LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    override fun createTimeSeries(): List<LocalDate> =
        (0..51).map { createDate().minusWeeks(it.toLong()) }

}

internal object MonthlyDateIntervalProvider : DateIntervalProvider {

    override fun createDate(): LocalDate =
        LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())

    override fun createTimeSeries(): List<LocalDate> =
        (0..11).map { createDate().minusMonths(it.toLong()) }

}

internal object YearlyDateIntervalProvider : DateIntervalProvider {

    override fun createDate(): LocalDate =
        LocalDate.now().with(TemporalAdjusters.firstDayOfYear())

    override fun createTimeSeries(): List<LocalDate> =
        listOf(createDate())

}

fun LocalDate.toUTCMillis(): Long =
    atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
