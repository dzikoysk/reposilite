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
package org.panda_lang.reposilite.stats

import org.panda_lang.reposilite.console.ReposiliteCommand
import org.panda_lang.utilities.commons.StringUtils
import org.panda_lang.utilities.commons.console.Effect.BLACK_BOLD
import org.panda_lang.utilities.commons.console.Effect.RESET
import org.panda_lang.utilities.commons.function.Option
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.util.concurrent.CompletableFuture
import java.util.function.BiPredicate

private const val DEFAULT_TOP_SIZE = 20

@Command(name = "stats", description = ["Display collected metrics"])
internal class StatsCommand(private val statisticsFacade: StatisticsFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "[<filter>]", description = ["accepts string as pattern and int as limiter"], defaultValue = "-1")
    private lateinit var filter: String

    override fun execute(output: MutableList<String>): Boolean {
        return try {
            loadAndProcessStats(output).get()
            true
        } catch (exception: Exception) {
            exception.printStackTrace()
            false
        }
    }

    private fun loadAndProcessStats(response: MutableList<String>): CompletableFuture<Void> {
        return statisticsFacade.loadAggregatedStats().thenAccept { aggregatedStats: AggregatedStats ->
            response.add("Statistics: ")
            response.add("  Unique requests: " + aggregatedStats.countUniqueRecords() + " (count: " + aggregatedStats.countRecords() + ")")

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

            response.add("  Recorded: " + (if (stats.isEmpty()) "[] " else "") + " (limiter: " + highlight(limiter) + ", pattern: '" + highlight(pattern) + "')")

            var order = 0

            for ((key, value) in stats) {
                response.add("    " + ++order + ". (" + value + ") " + key)

                if (limiter == -1 && order == DEFAULT_TOP_SIZE) {
                    break
                }
            }
        }
    }

    private fun highlight(value: Any): String {
        return BLACK_BOLD.toString() + value.toString() + RESET
    }
}