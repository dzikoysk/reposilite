package com.reposilite.failure

import com.reposilite.journalist.backend.InMemoryLogger

internal abstract class FailureSpec {

    protected val failureFacade = FailureFacade(InMemoryLogger())

}