package org.panda_lang.reposilite.auth;

import java.io.Serializable;
import java.util.List;

final class AuthDto implements Serializable {

    private final boolean isManager;
    private final String path;
    private final List<String> repositories;

    AuthDto(boolean isManager, String path, List<String> repositories) {
        this.isManager = isManager;
        this.path = path;
        this.repositories = repositories;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public String getPath() {
        return path;
    }

    public boolean isManager() {
        return isManager;
    }

}
