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

import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.utils.RunUtils;
import org.panda_lang.utilities.commons.function.ThrowingRunnable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

final class ReposiliteExecutor {

    private final boolean testEnvEnabled;
    private final FailureService failureService;
    private final Object lock = new Object();
    private final Queue<ThrowingRunnable<?>> tasks = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService;
    private volatile boolean alive = true;

    ReposiliteExecutor(boolean testEnvEnabled, FailureService failureService) {
        this.testEnvEnabled = testEnvEnabled;
        this.failureService = failureService;
        this.executorService = testEnvEnabled ? Executors.newSingleThreadExecutor() : null;
    }

    void await(Runnable onExit) throws InterruptedException {
        if (testEnvEnabled) {
            RunUtils.executeChecked(failureService, executorService, () -> start(onExit));
            return;
        }

        start(onExit);
    }

    public void start(Runnable onExit) throws InterruptedException {
        while (isAlive()) {
            Queue<ThrowingRunnable<?>> copy;

            synchronized (lock) {
                if (tasks.isEmpty()) {
                    lock.wait();
                }

                copy = new LinkedBlockingDeque<>(tasks);
                tasks.clear();
            }

            for (ThrowingRunnable<?> task : copy) {
                try {
                    task.run();
                } catch (Exception e) {
                    failureService.throwException("<executor>", e);
                }
            }
        }

        onExit.run();
    }

    void schedule(ThrowingRunnable<?> runnable) {
        synchronized (lock) {
            tasks.offer(runnable);
            lock.notifyAll();
        }
    }

    void stop() {
        synchronized (lock) {
            this.alive = false;
            lock.notifyAll();
        }
    }

    boolean isAlive() {
        return alive;
    }

}
