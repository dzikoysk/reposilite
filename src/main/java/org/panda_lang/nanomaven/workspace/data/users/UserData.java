package org.panda_lang.nanomaven.workspace.data.users;

import java.util.Map;

final class UserData {

    private Map<String, String> passwords;

    void setPasswords(Map<String, String> passwords) {
        this.passwords = passwords;
    }

    Map<String, String> getPasswords() {
        return passwords;
    }

}
