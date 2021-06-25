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

package org.panda_lang.reposilite.stats.infrastructure

import io.javalin.http.Context
import org.panda_lang.reposilite.stats.StatisticsFacade
import org.panda_lang.reposilite.stats.api.MAX_IDENTIFIER_LENGTH
import org.panda_lang.reposilite.stats.api.RecordType
import org.panda_lang.reposilite.web.RouteHandler
import org.panda_lang.reposilite.web.RouteMethod.BEFORE

internal class StatisticsHandler(private val statisticsFacade: StatisticsFacade) : RouteHandler {

    override val route = "*"
    override val methods = listOf(BEFORE)

    override fun handle(ctx: Context) {
        if (ctx.req.requestURI.length < MAX_IDENTIFIER_LENGTH) {
            statisticsFacade.increaseRecord(RecordType.REQUEST, ctx.req.requestURI)
        }
    }

}