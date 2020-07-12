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

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.AbstractFormatPatternWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * dirty af
 */
public final class ReposiliteWriter extends AbstractFormatPatternWriter {

    public static final int CACHE_SIZE = 100;
    private static final Queue<String> CACHE = new CircularFifoQueue<>(CACHE_SIZE);
    private static final Map<Object, Consumer<String>> CONSUMERS = new ConcurrentHashMap<>();

    public ReposiliteWriter(Map<String, String> properties) {
        super(properties);
    }

    @Override
    public void write(LogEntry logEntry) {
        String message = render(logEntry);
        CACHE.add(message);
        CONSUMERS.forEach((object, consumer) -> consumer.accept(message));
        System.out.print(message);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        clear();
    }

    public static void clear() {
        CACHE.clear();
        CONSUMERS.clear();
    }

    public static boolean contains(String message) {
        return getCache().stream()
                .filter(Objects::nonNull)
                .anyMatch(line -> line.contains(message));
    }

    public static List<String> getCache() {
        return new ArrayList<>(CACHE);
    }

    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        Collection<LogEntryValue> logEntryValues = super.getRequiredLogEntryValues();
        logEntryValues.add(LogEntryValue.LEVEL);
        return logEntryValues;
    }

    public static Map<Object, Consumer<String>> getConsumers() {
        return CONSUMERS;
    }

}
