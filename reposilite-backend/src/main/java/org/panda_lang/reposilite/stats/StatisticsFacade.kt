package org.panda_lang.reposilite.stats

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger

class StatisticsFacade(
    private val journalist: Journalist,
    private val statsService: StatsService
) : Journalist {

    override fun getLogger(): Logger = journalist.logger

}