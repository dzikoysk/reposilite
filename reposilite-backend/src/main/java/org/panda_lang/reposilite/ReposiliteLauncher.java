package org.panda_lang.reposilite;

import org.panda_lang.reposilite.console.HelpCommand;
import org.panda_lang.reposilite.console.VersionCommand;
import org.panda_lang.utilities.commons.console.Effect;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "reposilite", version = "Reposilite " + ReposiliteConstants.VERSION)
final class ReposiliteLauncher {

    @Option(names = { "--help", "-H" }, usageHelp = true, description = "display help message")
    private boolean usageHelpRequested;

    @Option(names = { "--version", "-V" }, versionHelp = true, description = "display current version of reposilite")
    private boolean versionInfoRequested;

    @Option(names = { "--test-env", "-te" }, description = "enable test mode")
    private boolean testEnv;

    @Option(names = { "--working-directory", "-wd" }, description = "set custom working directory of application instance")
    private String workingDirectory;

    public static void main(String[] args) throws Exception {
        ReposiliteLauncher launcher = new ReposiliteLauncher();
        launcher.launch(args);
    }

    public void launch(String... args) throws Exception {
        CommandLine.populateCommand(this, args);

        if (usageHelpRequested) {
            HelpCommand.displayHelp();
            return;
        }

        if (versionInfoRequested) {
            VersionCommand.displayVersion();
            return;
        }

        Reposilite reposilite = create(workingDirectory, testEnv);
        reposilite.launch();
    }

    public static Reposilite create(String workingDirectory, boolean testEnv) {
        Reposilite.getLogger().info("");
        Reposilite.getLogger().info(Effect.GREEN + "Reposilite " + Effect.RESET + ReposiliteConstants.VERSION);
        Reposilite.getLogger().info("");

        return new Reposilite(workingDirectory, testEnv);
    }

}
