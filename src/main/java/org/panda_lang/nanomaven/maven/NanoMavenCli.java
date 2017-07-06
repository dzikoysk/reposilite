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

package org.panda_lang.nanomaven.maven;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.maven.cli.MavenCli;
import org.panda_lang.nanomaven.NanoMaven;

import java.io.File;
import java.io.PrintStream;

public class NanoMavenCli {

    private final NanoMaven nanoMaven;
    private final MavenCli cli;
    private final PrintStream out;
    private final PrintStream err;

    public NanoMavenCli(NanoMaven nanoMaven) {
        this.nanoMaven = nanoMaven;
        this.cli = new MavenCli();
        this.out = new PrintStream(new ByteArrayOutputStream());
        this.err = new PrintStream(new ByteArrayOutputStream());
    }

    public int execute(File repository, String... args) {
        return execute(repository.getAbsolutePath(), args);
    }

    public int execute(String repository, String... args) {
        return cli.doMain(args, repository, out, err);
    }

}
