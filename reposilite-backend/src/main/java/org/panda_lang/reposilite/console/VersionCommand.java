package org.panda_lang.reposilite.console;

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteConstants;

public final class VersionCommand implements ReposiliteCommand {

    @Override
    public boolean call(Reposilite reposilite) {
        displayVersion();
        return true;
    }

    public static void displayVersion() {
        Reposilite.getLogger().info("Reposilite " + ReposiliteConstants.VERSION);
    }

}
