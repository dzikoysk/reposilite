package org.panda_lang.reposilite;

import org.panda_lang.utilities.commons.function.ThrowingRunnable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

final class ReposiliteExecutor {

    private final Reposilite reposilite;
    private final Object lock = new Object();
    private final Queue<ThrowingRunnable<?>> tasks = new ConcurrentLinkedQueue<>();
    private volatile boolean alive = true;

    ReposiliteExecutor(Reposilite reposilite) {
        this.reposilite = reposilite;
    }

    void await(Runnable onExit) throws InterruptedException {
        while (alive) {
            Queue<ThrowingRunnable<?>> copy;

            synchronized (lock) {
                lock.wait();
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
            lock.notify();
        }
    }

    void stop() {
        synchronized (lock) {
            this.alive = false;
            lock.notify();
        }
    }

}
