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

package org.panda_lang.nanomaven;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.shared.invoker.InvokerLogger;

import java.io.File;
import java.io.PrintStream;

public class NanoMavenCli {

    private final MavenCli cli;
    private final PrintStream out;
    private final PrintStream err;

    public NanoMavenCli() {
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

    public static class NanoInvokerLogger implements InvokerLogger {

        @Override
        public void debug(String s) {
            NanoMaven.getLogger().info(s);
        }

        @Override
        public void debug(String s, Throwable throwable) {
            NanoMaven.getLogger().info(s, throwable);
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public void info(String s) {
            NanoMaven.getLogger().info(s);
        }

        @Override
        public void info(String s, Throwable throwable) {
            NanoMaven.getLogger().info(s, throwable);
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void warn(String s) {
            NanoMaven.getLogger().info(s);
        }

        @Override
        public void warn(String s, Throwable throwable) {
            NanoMaven.getLogger().info(s, throwable);
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void error(String s) {
            NanoMaven.getLogger().info(s);
        }

        @Override
        public void error(String s, Throwable throwable) {
            NanoMaven.getLogger().info(s, throwable);
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public void fatalError(String s) {
            NanoMaven.getLogger().info(s);
        }

        @Override
        public void fatalError(String s, Throwable throwable) {
            NanoMaven.getLogger().info(s, throwable);
        }

        @Override
        public boolean isFatalErrorEnabled() {
            return true;
        }

        @Override
        public void setThreshold(int i) {

        }

        @Override
        public int getThreshold() {
            return 0;
        }

    }

}
