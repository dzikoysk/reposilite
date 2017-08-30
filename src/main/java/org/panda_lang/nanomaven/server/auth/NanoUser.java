/*
 * Copyright (c) 2017 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.nanomaven.server.auth;

import java.util.ArrayList;
import java.util.Collection;

public class NanoUser {

    private final String username;
    private String encryptedPassword;
    private Collection<NanoProject> projects;
    private boolean administrator;

    public NanoUser(String username) {
        this.username = username;
        this.projects = new ArrayList<>();
    }

    public void addProject(NanoProject project) {
        this.projects.add(project);
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void enableAdministrator() {
        this.administrator = true;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public Collection<NanoProject> getProjects() {
        return projects;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getUsername() {
        return username;
    }

}
