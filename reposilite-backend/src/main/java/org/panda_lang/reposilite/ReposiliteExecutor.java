package org.panda_lang.reposilite;

import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.utilities.commons.function.ThrowingRunnable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

final class ReposiliteExecutor {

    private final Reposilite reposilite;
    private final Object lock = new Object();
    private final Queue<ThrowingRunnable<?>> tasks = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService;
    private volatile boolean alive = true;

    ReposiliteExecutor(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.executorService = reposilite.isTestEnvEnabled() ? Executors.newSingleThreadExecutor() : null;
    }

    void await(Runnable onExit) throws InterruptedException {
        if (reposilite.isTestEnvEnabled()) {
            FutureUtils.submitChecked(executorService, () -> start(onExit));
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
                    reposilite.throwException("<executor>", e);
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
