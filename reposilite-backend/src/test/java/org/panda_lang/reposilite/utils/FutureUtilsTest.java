package org.panda_lang.reposilite.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FutureUtilsTest {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(1);

    @Test
    void submit() throws ExecutionException, InterruptedException {
        assertEquals("result", FutureUtils.submit(EXECUTOR_SERVICE, completableFuture -> completableFuture.complete("result")).get());
    }

}