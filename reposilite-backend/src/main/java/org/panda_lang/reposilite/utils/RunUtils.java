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

package org.panda_lang.reposilite.utils;

import org.panda_lang.reposilite.failure.FailureService;
import org.panda_lang.utilities.commons.function.ThrowingRunnable;

import java.util.concurrent.ExecutorService;

public final class RunUtils {

    private RunUtils() { }

    public static <E extends Exception> Runnable ofChecked(FailureService failureService, ThrowingRunnable<E> runnable) {
        return () -> run(failureService, runnable);
    }

    public static <E extends Exception> void executeChecked(FailureService failureService, ExecutorService service, ThrowingRunnable<E> runnable) {
        service.execute(() -> run(failureService, runnable));
    }

    private static void run(FailureService failureService, ThrowingRunnable<?> runnable) {
        try {
            runnable.run();
        } catch (Exception exception) {
            failureService.throwException("Exception occurred during the task execution", exception);
        }
    }

}
