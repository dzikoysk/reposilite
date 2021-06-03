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

package org.panda_lang.reposilite.stats.application

import io.javalin.Javalin
import net.dzikoysk.dynamiclogger.Journalist
import org.panda_lang.reposilite.console.ConsoleFacade
import org.panda_lang.reposilite.stats.StatisticsFacade
import org.panda_lang.reposilite.stats.StatsCommand
import org.panda_lang.reposilite.stats.api.RecordType
import org.panda_lang.reposilite.stats.infrastructure.SqlStatisticsRepository

internal object StatsWebConfiguration {

    fun createFacade(journalist: Journalist): StatisticsFacade {
        return StatisticsFacade(journalist, SqlStatisticsRepository())
    }

    fun configure(consoleFacade: ConsoleFacade, statisticsFacade: StatisticsFacade) {
        consoleFacade.registerCommand(StatsCommand(statisticsFacade))
    }

    fun installRouting(javalin: Javalin, statisticsFacade: StatisticsFacade) =
        javalin.before { ctx -> statisticsFacade.increaseRecord(RecordType.REQUEST, ctx.req.requestURI) }

}