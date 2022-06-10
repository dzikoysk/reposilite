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

import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import panda.std.Option
import panda.std.take
import panda.utilities.console.Effect.BLACK_BOLD
import panda.utilities.console.Effect.RESET
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

private const val DEFAULT_TOP_LIMIT = 20

@Command(name = "stats", description = ["Display collected metrics"])
internal class StatsCommand(private val statisticsFacade: StatisticsFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "[<repository>]", description = ["Repository to search in.", "By default it aggregates results from all repositories."], defaultValue = "")
    private lateinit var repository: String

    @Parameters(index = "1", paramLabel = "[<filter>]", description = ["Accepts string as pattern and int as limiter"], defaultValue = "")
    private lateinit var filter: String

    override fun execute(context: CommandContext) {
        context.append("Statistics: ")
        context.append("  Unique resolved requests: ${statisticsFacade.countUniqueRecords()}")
        context.append("  All resolved requests: ${statisticsFacade.countRecords()}")

        val limiter = Option.attempt(NumberFormatException::class.java) { filter.toInt() }.orElseGet(DEFAULT_TOP_LIMIT)
        val phrase = take(limiter != DEFAULT_TOP_LIMIT, "", filter)

        statisticsFacade.findResolvedRequestsByPhrase(repository, phrase, limiter)
            .peek { response ->
                context.append("Search results:")
                context.append("  Filter: '${highlight(phrase)}' / Limit: $limiter")
                if (repository.isNotEmpty()) context.append("  In repository: $repository")
                context.append("  Sum of matched requests: ${response.sum}")
                context.append("  Records:")
                response.requests.forEachIndexed { order, request -> context.append("    ${order + 1}. /${request.gav} (${request.count})") }
                if (response.requests.isEmpty()) context.append("    ~ Matching records not found ~")
            }
            .onError {
                context.append("Cannot fetch statistics: $it")
                context.status = FAILED
            }
    }

    private fun highlight(value: Any): String =
        BLACK_BOLD.toString() + value.toString() + RESET

}
