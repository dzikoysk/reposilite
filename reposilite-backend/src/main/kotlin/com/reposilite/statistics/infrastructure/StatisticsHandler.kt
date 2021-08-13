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

package com.reposilite.statistics.infrastructure

import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.MAX_IDENTIFIER_LENGTH
import com.reposilite.statistics.api.RecordType.REQUEST
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.BEFORE

internal class StatisticsHandler(private val statisticsFacade: StatisticsFacade) : ReposiliteRoutes() {

    private val collectRequests = ReposiliteRoute("/<*>", BEFORE) {
        if (ctx.req.requestURI.length < MAX_IDENTIFIER_LENGTH) {
            statisticsFacade.increaseRecord(REQUEST, ctx.req.requestURI)
        }
    }

    override val routes = setOf(collectRequests)

}