/*
 * Copyright (c) 2021 dzikoysk
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

import com.reposilite.console.CommandContext
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.statistics.api.RecordType
import panda.utilities.console.Effect.BLACK_BOLD
import panda.utilities.console.Effect.RESET
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

private const val DEFAULT_TOP_SIZE = 20

@Command(name = "stats", description = ["Display collected metrics"])
internal class StatsCommand(private val statisticsFacade: StatisticsFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "[<filter>]", description = ["Accepts string as pattern and int as limiter"], defaultValue = "")
    private lateinit var filter: String

    override suspend fun execute(context: CommandContext) {
        context.append("Statistics: ")
        context.append("  Unique requests: " + statisticsFacade.countUniqueRecords() + " (count: " + statisticsFacade.countRecords() + ")")

        val response = statisticsFacade.findRecordsByPhrase(RecordType.REQUEST, filter) // TOFIX: Limiter

        context.append("  Recorded: " + (if (response.count == 0L) "[] " else "") + " (pattern: '${highlight(filter)}')")
        response.records.forEachIndexed { order, record -> context.append("  ${order}. ${record.identifier} (${record.count})") }

        /*
            val limiter = Option.attempt(NumberFormatException::class.java) { filter!!.toInt() }
                .orElseGet(0)

            val pattern = if (limiter != 0) StringUtils.EMPTY else filter!!

            val stats = aggregatedStats.fetchStats(
                BiPredicate { uri: String?, counts: Int -> counts >= limiter },
                BiPredicate { uri: String, counts: Int? -> uri.contains(pattern) },
                BiPredicate { uri: String, counts: Int? -> !uri.endsWith(".md5") },
                BiPredicate { uri: String, counts: Int? -> !uri.endsWith(".sha1") },
                BiPredicate { uri: String, counts: Int? -> !uri.endsWith(".pom") },
                BiPredicate { uri: String, counts: Int? -> !uri.endsWith("/js/app.js") }
            )
         */
    }

    private fun highlight(value: Any): String =
        BLACK_BOLD.toString() + value.toString() + RESET

}