/*
 * Copyright (c) 2020 Dzikoysk
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

package org.panda_lang.nanomaven.console;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.fusesource.jansi.Ansi;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.NanoMavenCli.NanoInvokerLogger;
import org.panda_lang.nanomaven.repository.Artifact;
import org.panda_lang.nanomaven.repository.Repository;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public class ReinstallArtifactsCommand {

    public void execute(NanoMaven nanoMaven) throws Exception {
        int artifacts = 0;

        for (Repository repository : nanoMaven.getRepositoryService().getRepositories()) {
            File repositoryDirectory = new File(repository.getDirectory());
            File tempRepository = new File(repositoryDirectory.getParentFile(), "." + repository.getRepositoryName());

            if (!repositoryDirectory.renameTo(tempRepository)) {
                throw new RuntimeException("Cannot rename repository");
            }

            Invoker invoker = new DefaultInvoker();
            if (nanoMaven.getConfiguration().isNestedMaven()) {
                invoker.setMavenHome(new File("maven"));
            }
            else {
                invoker.setMavenHome(new File(nanoMaven.getConfiguration().getExternalMaven()));
            }
            invoker.setLogger(new NanoInvokerLogger());

            Collection< File > jars = FileUtils.listFiles(tempRepository, new String[]{ "jar" }, true);

            for (File jar : jars) {
                Artifact project = Artifact.fromPath(jar.getAbsolutePath());
                System.setProperty("maven.multiModuleProjectDirectory", jar.getParentFile().getAbsolutePath());

                String[] goals = new String[]{
                        "install:install-file",
                        "-Dfile=" + jar.getAbsolutePath(),
                        "-DgroupId=" + project.getGroup(),
                        "-DartifactId=" + project.getArtifact(),
                        "-Dversion=" + project.getVersion(),
                        "-Dpackaging=jar",
                        "-DcreateChecksum=true",
                        "-DlocalRepositoryPath=" + repositoryDirectory.getAbsolutePath() };

                InvocationRequest request = new DefaultInvocationRequest();
                request.setGoals(Arrays.asList(goals));
                InvocationResult result = invoker.execute(request);

                if (result.getExitCode() != 0) {
                    throw new RuntimeException("Cannot install artifact -> " + result.getExitCode() + "\n" + result.getExecutionException());
                }

                project.setRepository(repository.getRepositoryName());
                artifacts++;
            }

            if (!repositoryDirectory.exists()) {
                if (!tempRepository.renameTo(repositoryDirectory)) {
                    throw new RuntimeException("Cannot rename temp directory");
                }
            }
            else {
                FileUtils.forceDelete(tempRepository);
            }
        }

        NanoMaven.getLogger().info("");
        NanoMaven.getLogger().info(Ansi.ansi().bold().a(">> Reinstalled ").a(artifacts).a(" artifacts").boldOff().toString());
        NanoMaven.getLogger().info("");
    }

}
