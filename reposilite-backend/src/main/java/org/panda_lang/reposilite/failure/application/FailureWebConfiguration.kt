package org.panda_lang.reposilite.failure.application

import net.dzikoysk.dynamiclogger.Journalist
import org.panda_lang.reposilite.console.ConsoleFacade
import org.panda_lang.reposilite.failure.FailureFacade
import org.panda_lang.reposilite.failure.FailuresCommand

class FailureWebConfiguration {

    fun createFacade(journalist: Journalist) =
        FailureFacade(journalist)

    fun initialize(consoleFacade: ConsoleFacade, failureFacade: FailureFacade) {
        consoleFacade.registerCommand(FailuresCommand(failureFacade))
    }

    fun installRouting() {

    }

}