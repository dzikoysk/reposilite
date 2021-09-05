package com.reposilite.console

import com.reposilite.Reposilite
import com.reposilite.ReposiliteJournalist
import com.reposilite.VERSION
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.journalist.Channel
import com.reposilite.shared.TimeUtils
import com.reposilite.shared.createCommandHelp
import panda.std.Option.ofOptional
import panda.utilities.IOUtils
import panda.utilities.console.Effect.GREEN
import panda.utilities.console.Effect.GREEN_BOLD
import panda.utilities.console.Effect.RED_UNDERLINED
import panda.utilities.console.Effect.RESET
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.util.concurrent.TimeUnit.SECONDS

@Command(name = "help", aliases = ["?"], helpCommand = true, description = ["List of available commands"])
internal class HelpCommand(private val consoleFacade: ConsoleFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "[<command>]", description = ["display usage of the given command"], defaultValue = "")
    private lateinit var requestedCommand: String

    override fun execute(context: CommandContext) {
        createCommandHelp(consoleFacade.getCommands(), requestedCommand)
            .peek { context.appendAll(it) }
            .onError {
                context.append(it)
                context.status = FAILED
            }
    }

}


@Command(name = "stop", aliases = ["shutdown"], description = ["Shutdown server"])
internal class StopCommand(private val reposilite: Reposilite) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        reposilite.logger.warn("The shutdown request has been sent")
        reposilite.scheduler.schedule({ reposilite.shutdown() }, 1, SECONDS)
    }

}

@Command(name = "status", description = ["Display summary status of app health"])
internal class StatusCommand(
    private val reposilite: Reposilite,
    private val remoteVersionUrl: String,
) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        val latestVersion =
            if (reposilite.parameters.testEnv) VERSION
            else getVersion()

        context.append("Reposilite $VERSION Status")
        context.append("  Active: $GREEN_BOLD${reposilite.webServer.isAlive()}$RESET")
        context.append("  Uptime: ${TimeUtils.getPrettyUptimeInMinutes(reposilite.startTime)}")
        context.append("  Memory usage of process: ${memoryUsage()}")
        context.append("  Active threads in group: ${threadGroupUsage()}")
        context.append("  Recorded failures: ${reposilite.failureFacade.getFailures().size}")
        context.append("  Latest version of Reposilite: $latestVersion")
    }

    private fun getVersion(): String =
        IOUtils.fetchContent(remoteVersionUrl)
            .orElseGet { "$remoteVersionUrl is unavailable: ${it.message}" }
            .let { (if (VERSION == it) GREEN else RED_UNDERLINED).toString() + it + RESET }

    private fun memoryUsage(): String =
        TimeUtils.format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0) + "M"

    private fun threadGroupUsage(): String =
        Thread.activeCount().toString()

}

@Command(name = "level", description = ["Change current level of visible logging"])
internal class LevelCommand(private val journalist: ReposiliteJournalist) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<level>", description = ["the new threshold"], defaultValue = "info")
    private lateinit var level: String

    override fun execute(context: CommandContext) {
        ofOptional(Channel.of(level))
            .onEmpty {
                context.status = FAILED
                context.append("The new logging level has been set to $level")
            }
            .peek {
                journalist.setVisibleThreshold(it)
                context.append("The new logging level has been set to $level")
            }
    }

}