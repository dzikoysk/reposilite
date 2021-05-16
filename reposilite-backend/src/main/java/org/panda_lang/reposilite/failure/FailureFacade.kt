package org.panda_lang.reposilite.failure

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.utilities.commons.ArrayUtils
import org.panda_lang.utilities.commons.StringUtils
import java.util.concurrent.ConcurrentHashMap

class FailureFacade internal constructor(private val journalist: Journalist) : Journalist {

    private val exceptions: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun throwException(id: String, throwable: Throwable) {
        logger.error(id)
        logger.exception(throwable)

        exceptions
            .add(arrayOf(
                "failure $id",
                throwException(throwable)
            )
            .joinToString { System.lineSeparator() }
            .trim())
    }

    private fun throwException(throwable: Throwable?): String {
        if (throwable == null) {
            return StringUtils.EMPTY
        }

        return arrayOf(
            "  by ${throwable.javaClass.simpleName}: ${throwable.message}",
            "  at " + ArrayUtils.get(throwable.stackTrace, 0)
                .map { it.toString() }
                .orElseGet("<unknown stacktrace>"),
            throwException(throwable.cause)
        ).joinToString { System.lineSeparator() }
    }

    fun hasFailures() =
        exceptions.isNotEmpty()

    fun getFailures(): Collection<String> =
        exceptions

    override fun getLogger(): Logger =
        journalist.logger

}
