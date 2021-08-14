package com.reposilite.failure

import net.dzikoysk.dynamiclogger.backend.InMemoryLogger
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FailureFacadeTest {

    private val failureFacade = FailureFacade(InMemoryLogger())

    @Test
    fun `should store failure`() {
        // given: an exception with a message
        val message = "Unlucky"
        val exception = RuntimeException(message)

        // when: an error has been registered in failure facade
        failureFacade.throwException("PATH /com/reposilite", exception)

        // then: service properly registered thrown exception
        assertTrue(failureFacade.hasFailures())
        assertTrue(failureFacade.getFailures().iterator().next().contains(message))
    }

}