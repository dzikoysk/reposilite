package com.reposilite.failure.specification

import com.reposilite.failure.FailureFacade
import com.reposilite.journalist.backend.InMemoryLogger

internal abstract class FailureSpecification {

    protected val failureFacade = FailureFacade(InMemoryLogger())

}