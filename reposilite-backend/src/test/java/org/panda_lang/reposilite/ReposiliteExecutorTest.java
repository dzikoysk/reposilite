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

package org.panda_lang.reposilite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.utils.RunUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReposiliteExecutorTest {

    @TempDir
    File workingDirectory;

    @Test
    void shouldExecuteAndExit() throws InterruptedException {
        Reposilite reposilite = ReposiliteLauncher.create(null, workingDirectory.getAbsolutePath(), true);
        ReposiliteExecutor reposiliteExecutor = new ReposiliteExecutor(true, reposilite.getFailureService());
        AtomicBoolean onExitCalled = new AtomicBoolean(false);
        AtomicBoolean scheduleCalled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Thread asyncTest = new Thread(RunUtils.ofChecked(reposilite.getFailureService(), () -> {
            reposiliteExecutor.schedule(() -> {
                reposiliteExecutor.schedule(() -> scheduleCalled.set(true));
                reposiliteExecutor.schedule(reposiliteExecutor::stop);
            });

            reposiliteExecutor.await(() -> {
                onExitCalled.set(true);
                latch.countDown();
            });
        }));

        asyncTest.start();
        latch.await();

        assertFalse(reposiliteExecutor.isAlive());
        assertTrue(scheduleCalled.get());
        assertTrue(onExitCalled.get());
    }

}