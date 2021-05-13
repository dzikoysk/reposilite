package org.panda_lang.reposilite

import org.panda_lang.reposilite.auth.AuthenticationConfigurer
import org.panda_lang.reposilite.console.ConsoleConfigurer
import org.panda_lang.reposilite.metadata.MetadataConfigurer
import org.panda_lang.reposilite.stats.StatsConfigurer

fun configurations() = arrayOf(
    AuthenticationConfigurer(),
    ConsoleConfigurer(),
    MetadataConfigurer(),
    StatsConfigurer()
)