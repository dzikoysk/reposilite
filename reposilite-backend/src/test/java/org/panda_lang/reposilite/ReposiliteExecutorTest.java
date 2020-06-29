package org.panda_lang.reposilite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.utils.FutureUtils;

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
        Reposilite reposilite = new Reposilite(workingDirectory.getAbsolutePath(), true);
        ReposiliteExecutor reposiliteExecutor = new ReposiliteExecutor(reposilite);
        AtomicBoolean onExitCalled = new AtomicBoolean(false);
        AtomicBoolean scheduleCalled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Thread asyncTest = new Thread(FutureUtils.ofChecked(reposilite, () -> {
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