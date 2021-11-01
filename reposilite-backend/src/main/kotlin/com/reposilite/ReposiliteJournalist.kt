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

package com.reposilite

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.CachedLogger
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.journalist.backend.PublisherLogger
import com.reposilite.journalist.slf4j.Slf4jLogger
import com.reposilite.journalist.tinylog.TinyLogLogger
import com.reposilite.journalist.tinylog.TinyLogWriter
import org.eclipse.jetty.util.log.Log
import org.slf4j.LoggerFactory
import org.tinylog.provider.ProviderRegistry
import panda.std.reactive.Subscriber
import kotlin.collections.MutableMap.MutableEntry

/**
 * Initializes quite complicated flow of logging used in Reposilite.
 * The order of processing is as follows:
 *
 * 1. Message is sent to ReposiliteLogger
 * 2. Message is redirected to the standard SL4J implementation
 *   2.1 SLF4J writes every log entry to the log.txt file
 * 3. Message is caught by TinyLog wrapper and redirected to the rest of loggers
 *   3.1 Cached logger catches all messages and stores them in memory
 *   3.2 Visible logger prints messages that fulfills threshold requirements in the console
 */
class ReposiliteJournalist(
    visibleJournalist: Journalist,
    cachedLogSize: Int,
    private val testEnv: Boolean
) : Journalist {

    val cachedLogger = CachedLogger(Channel.ALL, cachedLogSize)

    private val publisherLogger = PublisherLogger(Channel.ALL)
    private val visibleLogger: Logger
    private val mainLogger: Logger
    private val tinyLog: TinyLogLogger

    init {
        this.visibleLogger = AggregatedLogger(visibleJournalist.logger, publisherLogger)
        setVisibleThreshold(Channel.INFO)

        if (!testEnv) {
            System.setProperty("tinylog.autoshutdown", "false")
        }

        val redirectedLogger = AggregatedLogger(cachedLogger, visibleLogger)
        this.tinyLog = TinyLogLogger(Channel.ALL, redirectedLogger) // Redirect TinyLog output to redirected loggers

        Log.getProperties().setProperty("org.eclipse.jetty.util.log.announce", "false")
        this.mainLogger = if (testEnv) InMemoryLogger() else Slf4jLogger(LoggerFactory.getLogger(Reposilite::class.java))
    }

    fun subscribe(subscriber: Subscriber<MutableEntry<Channel, String>>): Int =
        publisherLogger.subscribe(subscriber)

    fun unsubscribe(subscriberId: Int): Boolean =
        publisherLogger.unsubscribe(subscriberId)

    fun setVisibleThreshold(channel: Channel) {
        visibleLogger.setThreshold(channel)
    }

    fun shutdown() {
        TinyLogWriter.unsubscribe(tinyLog.subscriberId)

        if (!testEnv) {
            ProviderRegistry.getLoggingProvider().shutdown()
        }
    }

    override fun getLogger(): Logger =
        mainLogger

}