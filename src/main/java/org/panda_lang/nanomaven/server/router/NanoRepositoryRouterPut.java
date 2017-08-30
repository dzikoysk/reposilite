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

package org.panda_lang.nanomaven.server.router;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.NanoHTTPD.ResponseException;
import org.apache.commons.io.FileUtils;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.NanoMavenConstants;
import org.panda_lang.nanomaven.server.NanoHttpdServer;
import org.panda_lang.nanomaven.server.NanoRouter;
import org.panda_lang.nanomaven.server.auth.NanoProject;
import org.panda_lang.nanomaven.server.auth.NanoProjectsManager;
import org.panda_lang.nanomaven.server.auth.NanoUser;
import org.panda_lang.nanomaven.server.auth.NanoUsersManager;
import org.panda_lang.nanomaven.workspace.configuration.NanoMavenConfiguration;
import org.panda_lang.nanomaven.workspace.data.users.NanoUserDatabase;
import org.panda_lang.nanomaven.workspace.repository.NanoRepositoryFile;
import org.panda_lang.nanomaven.workspace.repository.NanoRepositoryProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class NanoRepositoryRouterPut implements NanoRouter {

    @Override
    public NanoHTTPD.Response serve(NanoHttpdServer server, NanoHTTPD.IHTTPSession session) throws Exception {
        NanoMaven nanoMaven = server.getNanoMaven();
        NanoMavenConfiguration configuration = nanoMaven.getConfiguration();

        if (!configuration.isDeployEnabled()) {
            return result(session, Status.INTERNAL_ERROR, "Artifact deployment is disabled");
        }

        NanoUser user = null;

        if (configuration.isAuthorizationEnabled()) {
            String authorization = session.getHeaders().get("authorization");

            if (authorization == null) {
                return result(session, Status.UNAUTHORIZED, "Authorization credentials are not specified");
            }

            if (!authorization.startsWith("Basic")) {
                return result(session, Status.UNAUTHORIZED, "Unknown auth method");
            }

            String base64Credentials = authorization.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));

            String[] values = credentials.split(":", 2);
            String username = values[0];
            String password = values[1];

            NanoUsersManager usersManager = nanoMaven.getUsersManager();
            user = usersManager.getUser(username);

            if (user == null) {
                return result(session, Status.UNAUTHORIZED, "Invalid authorization credentials");
            }

            boolean authorized = NanoUserDatabase.B_CRYPT_PASSWORD_ENCODER.matches(password, user.getEncryptedPassword());

            if (!authorized) {
                return result(session, Status.UNAUTHORIZED, "Invalid authorization credentials");
            }

            NanoMaven.getLogger().info(username + " (" + session.getRemoteIpAddress() + ") authorized");
        }

        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (IOException | ResponseException e) {
            e.printStackTrace();
            return result(session, Status.BAD_REQUEST, "Cannot parse body");
        }

        for (Map.Entry<String, String> entry : files.entrySet()){
            File tempFile = new File(entry.getValue());
            NanoRepositoryFile targetFile = NanoRepositoryFile.fromURL(session.getUri());

            if (configuration.isAuthorizationEnabled()) {
                if (user == null) {
                    return result(session, Status.INTERNAL_ERROR, "User cannot be null");
                }

                if (!user.isAdministrator()) {
                    NanoProjectsManager projectsManager = nanoMaven.getProjectsManager();
                    NanoRepositoryProject project = targetFile.toRepositoryProject();
                    NanoProject nanoProject = project.toNanoProject(projectsManager);

                    if (nanoProject == null) {
                        return result(session, Status.UNAUTHORIZED, "Project doesn't have any extra data (like project members)");
                    }

                    if (!nanoProject.containsUser(user)) {
                        return result(session, Status.UNAUTHORIZED, "User doesn't belong to the project");
                    }
                }
            }

            FileUtils.forceMkdirParent(targetFile.getFile());
            Files.copy(tempFile.toPath(), targetFile.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Success");
    }

    // Maven doesn't listen for response
    private Response result(IHTTPSession session, Status status, String response) throws Exception {
        if (NanoMavenConstants.INTERNAL_DEBUG) {
            NanoMaven.getLogger().info(status.name() + ": " + response);
        }

        return null; // mute
    }

}
