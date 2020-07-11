package org.panda_lang.reposilite;

import org.panda_lang.reposilite.console.HelpCommand;
import org.panda_lang.reposilite.console.VersionCommand;
import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.utilities.commons.StringUtils;
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

    @Option(names = { "--config", "-cfg" }, description = "set custom location of configuration file")
    private String configurationFile;

    public static void main(String... args) {
        create(args).ifPresent(reposilite -> FutureUtils.ofChecked(reposilite, reposilite::launch).run());
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

        return Optional.of(create(launcher.configurationFile, launcher.workingDirectory, launcher.testEnv));
    }

    public static Reposilite create(String configurationFile, String workingDirectory, boolean testEnv) {
        Reposilite.getLogger().info("");
        Reposilite.getLogger().info(Effect.GREEN + "Reposilite " + Effect.RESET + ReposiliteConstants.VERSION);
        Reposilite.getLogger().info("");

        if (configurationFile == null) {
            configurationFile = StringUtils.EMPTY;
        }

        if (workingDirectory == null) {
            workingDirectory = StringUtils.EMPTY;
        }

        return new Reposilite(configurationFile, workingDirectory, testEnv);
    }

}
