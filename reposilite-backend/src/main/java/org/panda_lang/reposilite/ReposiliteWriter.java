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
