package org.panda_lang.reposilite;

import com.google.common.collect.EvictingQueue;
import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.provider.InternalLogger;
import org.tinylog.writers.AbstractFormatPatternWriter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * dirty af
 */
public final class ReposiliteWriter extends AbstractFormatPatternWriter {

    @SuppressWarnings("UnstableApiUsage")
    private static final Queue<String> LATEST = EvictingQueue.create(100);
    private static final Map<Object, Consumer<String>> CONSUMERS = new ConcurrentHashMap<>();

    private final Level level;

    public ReposiliteWriter() {
        this(Collections.emptyMap());
    }

    public ReposiliteWriter(Map<String, String> properties) {
        super(properties);
        String stream = properties.get("stream");

        if (stream == null) {
            level = Level.WARN;
        }
        else if ("err".equalsIgnoreCase(stream)) {
            level = Level.TRACE;
        }
        else if ("out".equalsIgnoreCase(stream)) {
            level = Level.OFF;
        }
        else {
            InternalLogger.log(Level.WARN, "Logging stream is not defined");
            level = Level.WARN;
        }
    }

    @Override
    public void write(LogEntry logEntry) {
        String message = render(logEntry);
        LATEST.add(message);
        CONSUMERS.forEach((object, consumer) -> consumer.accept(message));

        if (logEntry.getLevel().ordinal() < level.ordinal()) {
            System.out.print(message);
            return;
        }

        System.err.print(message);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

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

    public static Queue<String> getLatest() {
        return LATEST;
    }

}
