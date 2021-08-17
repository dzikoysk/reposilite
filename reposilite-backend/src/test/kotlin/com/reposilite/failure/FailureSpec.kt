package com.reposilite.failure

import net.dzikoysk.dynamiclogger.backend.InMemoryLogger

internal abstract class FailureSpec {

    protected val failureFacade = FailureFacade(InMemoryLogger())

}