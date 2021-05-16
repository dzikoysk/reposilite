package org.panda_lang.reposilite.token.application

import net.dzikoysk.dynamiclogger.Journalist
import org.panda_lang.reposilite.token.AccessTokenFacade
import org.panda_lang.reposilite.token.infrastructure.SqlAccessTokenRepository

class AccessTokenWebConfiguration {

    fun createFacade(journalist: Journalist): AccessTokenFacade {
        return AccessTokenFacade(journalist, SqlAccessTokenRepository())
    }

}