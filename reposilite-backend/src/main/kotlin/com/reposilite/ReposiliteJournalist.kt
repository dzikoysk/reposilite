package com.reposilite

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.CachedLogger
import com.reposilite.journalist.backend.PublisherLogger
import com.reposilite.journalist.slf4j.Slf4jLogger
import com.reposilite.journalist.tinylog.TinyLogLogger
import org.slf4j.LoggerFactory
import panda.std.Subscriber
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
    cachedLogSize: Int
) : Journalist {

    val cachedLogger = CachedLogger(Channel.ALL, cachedLogSize)

    private val mainLogger = Slf4jLogger(LoggerFactory.getLogger(Reposilite::class.java))
    private val publisherLogger = PublisherLogger(Channel.ALL)
    private val visibleLogger = AggregatedLogger(visibleJournalist.logger, publisherLogger)
    private val redirectedLogger = AggregatedLogger(cachedLogger, visibleLogger)

    init {
        TinyLogLogger(Channel.ALL, redirectedLogger) // Redirect TinyLog output to redirected loggers
    }

    fun subscribe(subscriber: Subscriber<MutableEntry<Channel, String>>): Int =
        publisherLogger.subscribe(subscriber)

    fun unsubscribe(subscriberId: Int): Boolean =
        publisherLogger.unsubscribe(subscriberId)

    fun setVisibleThreshold(channel: Channel) {
        visibleLogger.setThreshold(channel)
    }

    override fun getLogger(): Logger =
        mainLogger

}