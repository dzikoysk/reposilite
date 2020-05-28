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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public final class ExecutorsUtils {

    private ExecutorsUtils() { }

    public static <T> CompletableFuture<T> submit(ExecutorService service, Function<CompletableFuture<T>, ?> futureConsumer) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        service.submit(() -> futureConsumer.apply(completableFuture));
        return completableFuture;
    }

}
