package com.reposilite.status.application

import com.reposilite.journalist.Journalist
import com.reposilite.status.FailureFacade

class FailureComponents(private val journalist: Journalist) {

    fun failureFacade(): FailureFacade =
        FailureFacade(journalist)

}
