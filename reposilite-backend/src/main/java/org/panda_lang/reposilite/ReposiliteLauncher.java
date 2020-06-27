package org.panda_lang.reposilite;

import io.vavr.control.Try;
import org.panda_lang.reposilite.console.HelpCommand;
import org.panda_lang.reposilite.console.VersionCommand;
import org.panda_lang.utilities.commons.console.Effect;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Optional;

@Command(name = "reposilite", version = "Reposilite " + ReposiliteConstants.VERSION)
public final class ReposiliteLauncher {

    @Option(names = { "--help", "-H" }, usageHelp = true, description = "display help message")
    private boolean usageHelpRequested;

    @Option(names = { "--version", "-V" }, versionHelp = true, description = "display current version of reposilite")
    private boolean versionInfoRequested;

    @Option(names = { "--test-env", "-te" }, description = "enable test mode")
    private boolean testEnv;

    @Option(names = { "--working-directory", "-wd" }, description = "set custom working directory of application instance")
    private String workingDirectory;

    public static void main(String[] args) {
        create(args).ifPresent(reposilite -> Try.run(reposilite::launch).orElseRun(Throwable::printStackTrace));
    }

    public static Optional<Reposilite> create(String... args) {
        ReposiliteLauncher launcher = CommandLine.populateCommand(new ReposiliteLauncher(), args);

        if (launcher.usageHelpRequested) {
            HelpCommand.displayHelp();
            return Optional.empty();
        }

        if (launcher.versionInfoRequested) {
            VersionCommand.displayVersion();
            return Optional.empty();
        }

        return Optional.of(create(launcher.workingDirectory, launcher.testEnv));
    }

    public static Reposilite create(String workingDirectory, boolean testEnv) {
        Reposilite.getLogger().info("");
        Reposilite.getLogger().info(Effect.GREEN + "Reposilite " + Effect.RESET + ReposiliteConstants.VERSION);
        Reposilite.getLogger().info("");

        return new Reposilite(workingDirectory, testEnv);
    }

}
