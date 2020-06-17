package org.panda_lang.reposilite.auth;

import java.io.Serializable;

final class AuthDto implements Serializable {

    private final String path;
    private final boolean isManager;

    AuthDto(String path, boolean isManager) {
        this.path = path;
        this.isManager = isManager;
    }

    public boolean isManager() {
        return isManager;
    }

    public String getPath() {
        return path;
    }

}
