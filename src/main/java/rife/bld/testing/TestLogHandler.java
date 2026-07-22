/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.testing;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * Thread-safe custom log handler for capturing log messages during tests.
 *
 * <h3>Usage Examples:</h3>
 *
 * <blockquote><pre>
 * // Using the LoggingExtension
 * private static final Logger logger = Logger.getLogger(MyClass.class.getName());
 * private static final TestLogHandler testLogHandler = new TestLogHandler();
 *
 * &#64;RegisterExtension
 * &#64;SuppressWarnings("unused")
 * private static final LoggingExtension loggingExtension = new LoggingExtension(
 *     logger,
 *     testLogHandler,
 *     Level.ALL
 * );
 *
 * // Manually, in a test method
 * &#64;Test
 * void testMethod() {
 *     var logger = Logger.getLogger(MyClass.class.getName());
 *     var logHandler = new TestLogHandler();
 *
 *     logger.addHandler(logHandler);
 *     logger.setLevel(Level.ALL);
 *
 *     // ...
 *
 *     logger.removeHandler(logHandler);
 * }</pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see LoggingExtension
 * @since 1.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestLogHandler extends Handler {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final List<LogRecord> logRecords = new CopyOnWriteArrayList<>();

    /**
     * Publishes a log record if the handler is not closed.
     *
     * @param record description of the log event. A null record is silently ignored and is not published
     */
    @Override
    public void publish(LogRecord record) {
        if (record == null || closed.get() || !isLoggable(record)) {
            return;
        }
        logRecords.add(record);
    }

    /**
     * Flushes this log handler.
     * <p>
     * No-op implementation as records are immediately available.
     */
    @Override
    public void flush() {
        // no-op - records are immediately available
    }

    /**
     * Closes this log handler and prevents further logging.
     * <p>
     * Captured records are retained after closing and can still be read.
     * Use {@link #clear()} to discard them explicitly.
     * <p>
     * Thread-safe operation.
     */
    @Override
    public void close() {
        closed.set(true);
    }

    /**
     * Clears all captured log records and messages.
     * <p>
     * Thread-safe operation.
     */
    public void clear() {
        logRecords.clear();
    }

    /**
     * Checks if the log contains the exact message.
     *
     * @param message the message to check for
     * @return {@code true} if the log contains the message, {@code false} otherwise
     */
    public boolean containsExactMessage(String message) {
        return message != null && logRecords.stream().anyMatch(record -> message.equals(record.getMessage()));
    }

    /**
     * Checks if the log contains a message containing the given text.
     *
     * @param message the text to check for
     * @return {@code true} if the log contains a message with the text, {@code false} otherwise
     */
    public boolean containsMessage(String message) {
        return message != null && logRecords.stream().anyMatch(record ->
                record.getMessage() != null && record.getMessage().contains(message));
    }

    /**
     * Checks if the log contains a message matching the given regex pattern.
     *
     * @param pattern the regex pattern to match against
     * @return {@code true} if any message matches the pattern, {@code false} otherwise
     */
    public boolean containsMessageMatching(Pattern pattern) {
        return pattern != null && logRecords.stream().anyMatch(record ->
                record.getMessage() != null && pattern.matcher(record.getMessage()).find());
    }

    /**
     * Counts the number of messages containing the given text.
     *
     * @param message the text to check for
     * @return the number of messages containing the given text
     */
    public long countMessagesContaining(String message) {
        return TestingUtils.isEmpty(message) ? 0 :
                logRecords.stream().filter(
                        record -> record.getMessage() != null
                                && record.getMessage().contains(message)).count();
    }

    /**
     * Counts the number of log records at the specified level.
     *
     * @param level the log level to count
     * @return the number of records at the specified level
     */
    public long countRecordsAtLevel(Level level) {
        return level == null ? 0 :
                logRecords.stream().filter(record -> level.equals(record.getLevel())).count();
    }

    /**
     * Gets the first log record containing the given text.
     *
     * @param message the text to check for
     * @return an {@link Optional} containing the first log record with the given text,
     * or {@link Optional#empty()} if not found or message is empty
     */
    public Optional<LogRecord> getFirstRecordContaining(String message) {
        if (!TestingUtils.isEmpty(message)) {
            return logRecords.stream()
                    .filter(record -> record.getMessage() != null && record.getMessage().contains(message))
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the most recent log record, if any.
     *
     * @return an {@link Optional} containing the most recent log record,
     * or {@link Optional#empty()} if no records exist
     */
    public Optional<LogRecord> getLastRecord() {
        var snapshot = List.copyOf(logRecords);
        return snapshot.isEmpty() ? Optional.empty() : Optional.of(snapshot.get(snapshot.size() - 1));
    }

    /**
     * Gets the last log record containing the given text.
     * <p>
     * Uses a snapshot of the current records to avoid race conditions during concurrent access.
     *
     * @param message the text to check for
     * @return an {@link Optional} containing the last log record with the given text,
     * or {@link Optional#empty()} if not found or message is empty
     */
    public Optional<LogRecord> getLastRecordContaining(String message) {
        if (TestingUtils.isEmpty(message)) {
            return Optional.empty();
        }
        // Snapshot first to avoid TOCTOU race between size() and get(i) on the live list
        var snapshot = List.copyOf(logRecords);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            LogRecord record = snapshot.get(i);
            if (record.getMessage() != null && record.getMessage().contains(message)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all captured log messages as strings.
     * <p>
     * Returns an immutable snapshot of current messages. Records with a {@code null} message
     * are represented as empty strings.
     *
     * @return immutable list of log messages
     */
    public List<String> getLogMessages() {
        return logRecords.stream()
                .map(record -> record.getMessage() != null ? record.getMessage() : "")
                .toList();
    }

    /**
     * Gets all captured log records.
     * <p>
     * Returns an immutable snapshot of current records.
     *
     * @return immutable list of log records
     */
    public List<LogRecord> getLogRecords() {
        return List.copyOf(logRecords);
    }

    /**
     * Gets the total number of captured log records.
     *
     * @return the number of log records
     */
    public int getRecordCount() {
        return logRecords.size();
    }

    /**
     * Gets all log records at or above the specified level.
     *
     * @param level the minimum log level
     * @return immutable list of log records at or above the specified level
     */
    public List<LogRecord> getRecordsAtOrAboveLevel(Level level) {
        if (level == null) {
            return List.of();
        }
        return logRecords.stream()
                .filter(record -> record.getLevel() != null
                        && record.getLevel().intValue() >= level.intValue())
                .toList();
    }

    /**
     * Checks if the log contains a record with the given level.
     *
     * @param level the level to check for
     * @return {@code true} if the log contains a record with the given level, {@code false} otherwise
     */
    public boolean hasLogLevel(Level level) {
        return level != null && logRecords.stream().anyMatch(record -> level.equals(record.getLevel()));
    }

    /**
     * Checks if this handler has been closed.
     *
     * @return {@code true} if the handler is closed, {@code false} otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Checks if any log records have been captured.
     *
     * @return {@code true} if no records have been captured, {@code false} otherwise
     */
    public boolean isEmpty() {
        return logRecords.isEmpty();
    }

    /**
     * Prints all captured log messages to standard output.
     * <p>
     * Each message is printed on a separate line in the order it was logged.
     * Records with a {@code null} message are printed as empty lines.
     */
    public void printLogMessages() {
        getLogMessages().forEach(System.out::println);
    }

    /**
     * Prints all captured log messages to the specified output stream.
     * <p>
     * Each message is printed on a separate line in the order it was logged.
     * Records with a {@code null} message are printed as empty lines.
     *
     * @param out the output stream to print to
     */
    public void printLogMessages(java.io.PrintStream out) {
        if (out != null) {
            getLogMessages().forEach(out::println);
        }
    }

    /**
     * Prints all captured log messages with their log levels to standard output.
     * <p>
     * Each message is printed in the format: [LEVEL] message
     */
    @SuppressWarnings("PMD.SystemPrintln")
    public void printLogMessagesWithLevel() {
        logRecords.forEach(record ->
                System.out.println("[" + record.getLevel() + "] " + record.getMessage()));
    }

    /**
     * Prints all captured log messages with their log levels to the specified output stream.
     * <p>
     * Each message is printed in the format: [LEVEL] message
     *
     * @param out the output stream to print to
     */
    public void printLogMessagesWithLevel(java.io.PrintStream out) {
        if (out != null) {
            logRecords.forEach(record ->
                    out.println("[" + record.getLevel() + "] " + record.getMessage()));
        }
    }

    /**
     * Prints all captured log messages with both level and timestamp to standard output.
     * <p>
     * Each message is printed in the format: [timestamp] [LEVEL] message
     */
    @SuppressWarnings("PMD.SystemPrintln")
    public void printLogMessagesWithLevelAndTimestamp() {
        logRecords.forEach(record ->
                System.out.println("[" + java.time.Instant.ofEpochMilli(record.getMillis()) + "] [" +
                        record.getLevel() + "] " + record.getMessage()));
    }

    /**
     * Prints all captured log messages with both level and timestamp to the specified output stream.
     * <p>
     * Each message is printed in the format: [timestamp] [LEVEL] message
     *
     * @param out the output stream to print to
     */
    public void printLogMessagesWithLevelAndTimestamp(java.io.PrintStream out) {
        if (out != null) {
            logRecords.forEach(record ->
                    out.println("[" + java.time.Instant.ofEpochMilli(record.getMillis()) + "] [" +
                            record.getLevel() + "] " + record.getMessage()));
        }
    }

    /**
     * Prints all captured log messages with timestamps to standard output.
     * <p>
     * Each message is printed in the format: [timestamp] message
     */
    @SuppressWarnings("PMD.SystemPrintln")
    public void printLogMessagesWithTimestamp() {
        logRecords.forEach(record ->
                System.out.println("[" + java.time.Instant.ofEpochMilli(record.getMillis()) + "] " +
                        record.getMessage()));
    }

    /**
     * Prints all captured log messages with timestamps to the specified output stream.
     * <p>
     * Each message is printed in the format: [timestamp] message
     *
     * @param out the output stream to print to
     */
    public void printLogMessagesWithTimestamp(java.io.PrintStream out) {
        if (out != null) {
            logRecords.forEach(record ->
                    out.println("[" + java.time.Instant.ofEpochMilli(record.getMillis()) + "] " +
                            record.getMessage()));
        }
    }
}