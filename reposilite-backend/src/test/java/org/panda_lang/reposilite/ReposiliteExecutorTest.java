package org.panda_lang.reposilite;

import io.vavr.control.Try;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReposiliteExecutorTest {

    @Test
    void shouldExecuteAndExit() throws InterruptedException {
        ReposiliteExecutor reposiliteExecutor = new ReposiliteExecutor(new Reposilite("", true));
        AtomicBoolean onExitCalled = new AtomicBoolean(false);
        AtomicBoolean scheduleCalled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            Try.run(() -> {
                reposiliteExecutor.schedule(() -> {
                    reposiliteExecutor.schedule(() -> scheduleCalled.set(true));
                    reposiliteExecutor.schedule(reposiliteExecutor::stop);
                });

                reposiliteExecutor.await(() -> onExitCalled.set(true));
                latch.countDown();
            }).onFailure(Throwable::printStackTrace);
        }).start();

        latch.await();
        assertFalse(reposiliteExecutor.isAlive());
        assertTrue(scheduleCalled.get());
        assertTrue(onExitCalled.get());
    }

}