package org.panda_lang.reposilite

import org.panda_lang.reposilite.auth.AuthenticationConfiguration
import org.panda_lang.reposilite.console.ConsoleConfiguration
import org.panda_lang.reposilite.metadata.MetadataConfiguration
import org.panda_lang.reposilite.stats.StatsConfiguration

fun configurations() = arrayOf(
    AuthenticationConfiguration(),
    ConsoleConfiguration(),
    MetadataConfiguration(),
    StatsConfiguration()
)