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

import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for captured stdout and stderr output during test execution.
 * <p>
 * This class provides methods to analyze and inspect console output that was
 * captured during a test method annotated with {@link CaptureOutput}.
 * <p>
 * Instances of this class are automatically created by the {@link CaptureOutputExtension}
 * and injected into test methods as parameters. The class offers various methods to
 * access and analyze the captured output:
 * <p>
 * All captured output is decoded using {@code UTF-8}
 *
 * <ul>
 *     <li>String access methods: {@link #getOut()}, {@link #getErr()}, {@link #getAll()}</li>
 *     <li>Line access methods: {@link #getOutLines()}, {@link #getErrLines()}, {@link #getAllLines()}</li>
 *     <li>Byte array access methods: {@link #getOutAsBytes()}, {@link #getErrAsBytes()}</li>
 *     <li>Chronological access methods: {@link #getChronologicalEntries()}, {@link #getChronologicalContent()}</li>
 *     <li>Utility methods: {@link #isEmpty()}, {@link #contains(String)}, {@link #outContains(String)},
 *     {@link #errContains(String)}</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 *
 * <blockquote><pre>
 * &#64;Test
 * &#64;CaptureOutput
 * void testConsoleOutput(CapturedOutput output) {
 *     System.out.println("Hello World");
 *     System.err.println("Error");
 *
 *     // Access individual streams
 *     assertEquals("Hello World\n", output.getOut());
 *     assertEquals("Error\n", output.getErr());
 *
 *     // Access chronological output
 *     List&lt;OutputEntry&gt; entries = output.getChronologicalEntries();
 *     assertEquals(OutputType.STDOUT, entries.get(0).getType());
 *     assertEquals("Hello World\n", entries.get(0).getContent());
 *
 *     // Get chronological content as single string
 *     String chronological = output.getChronologicalContent();
 *
 *     // Search within streams
 *     assertTrue(output.outContains("Hello"));
 *     assertTrue(output.errContains("Error"));
 *     assertTrue(output.contains("World"));
 *
 *     // Check if output is empty
 *     assertFalse(output.isEmpty());
 * }</pre></blockquote>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is not thread-safe. Each instance
 * should only be used within the context of a single test method execution.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see CaptureOutput
 * @see CaptureOutputExtension
 * @since 1.0
 */
@NullMarked
public class CapturedOutput {

    /**
     * The chronologically ordered list of output entries with timestamps and types.
     */
    private final List<OutputEntry> chronologicalEntries;
    /**
     * The ByteArrayOutputStream containing captured stderr data.
     */
    private final ByteArrayOutputStream stderr;
    /**
     * The ByteArrayOutputStream containing captured stdout data.
     */
    private final ByteArrayOutputStream stdout;

    /**
     * Creates a new CapturedOutput instance with the specified output streams.
     * <p>
     * This constructor is used internally by the {@link CaptureOutputExtension}
     * to wrap the captured stdout and stderr streams. It is not intended for direct
     * use by test code.
     *
     * @param stdout the ByteArrayOutputStream containing captured stdout data
     * @param stderr the ByteArrayOutputStream containing captured stderr data
     */
    CapturedOutput(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.chronologicalEntries = new ArrayList<>();
    }

    /**
     * Provides a string representation of this captured output for debugging purposes.
     * <p>
     * The returned string includes both the stdout and stderr content in a
     * structured format that's useful for debugging test failures. The format
     * shows the class name and both captured streams.
     * <p>
     * Example output:<br>
     * {@code CapturedOutput{stdout='Hello World\n', stderr='Error\n'}}
     *
     * @return a string representation of the captured output
     */
    @Override
    public String toString() {
        return "CapturedOutput{" +
                "stdout='" + getOut() + '\'' +
                ", stderr='" + getErr() + '\'' +
                ", chronologicalEntries=" + chronologicalEntries.size() + " entries" +
                '}';
    }

    /**
     * Searches for the specified text within both stdout and stderr content.
     * <p>
     * This method performs a case-sensitive substring search within both
     * the stdout and stderr content. It returns {@code true} if the text is
     * found in either stream. This is useful when you don't care which specific
     * stream contains the text, just that it was output somewhere.
     * <p>
     * This method is equivalent to:<br>
     * {@code outContains(text) || errContains(text)}
     *
     * @param text the text to search for in either stdout or stderr (must not be {@code null})
     * @return {@code true} if either stdout or stderr contains the specified text, {@code false} otherwise
     * @throws NullPointerException if text is {@code null}
     * @see #outContains(String)
     * @see #errContains(String)
     */
    public boolean contains(String text) {
        return outContains(text) || errContains(text);
    }

    /**
     * Searches for the specified text within the captured stderr content.
     * <p>
     * This method performs a case-sensitive substring search within the
     * stderr content. It's equivalent to calling {@code getErr().contains(text)}
     * but provides a more readable API for common assertions.
     *
     * @param text the text to search for in stderr (must not be {@code null})
     * @return {@code true} if stderr contains the specified text, {@code false} otherwise
     * @throws NullPointerException if text is {@code null}
     * @see #outContains(String)
     * @see #contains(String)
     * @see String#contains(CharSequence)
     */
    public boolean errContains(String text) {
        return getErr().contains(text);
    }

    /**
     * Combines and retrieves both stdout and stderr content as a single string.
     * <p>
     * This method concatenates the stdout content followed by the stderr content
     * exactly as captured, without inserting any synthetic separators. The content
     * is preserved verbatim.
     * <p>
     * <strong>Note:</strong> This does not preserve the exact interleaving of stdout and stderr
     * as it would appear in a real console, but rather groups all stdout first,
     * then all stderr. For chronological ordering, use {@link #getChronologicalContent()}.
     *
     * @return the combined stdout and stderr content, or empty string if no output was captured
     * @see #getOut()
     * @see #getErr()
     * @see #getChronologicalContent()
     */
    public String getAll() {
        return getOut() + getErr();
    }

    /**
     * Retrieves the combined stdout and stderr content as a list of lines.
     * <p>
     * This method splits the combined output (stdout followed by stderr) into
     * individual lines using Java's built-in line processing. Empty lines
     * are preserved in the result. The line splitting handles various line
     * separator formats (\n, \r\n, \r) correctly across different platforms.
     * <p>
     * <strong>Note:</strong> For chronological line ordering, use {@link #getChronologicalLines()}.
     *
     * @return a list of lines from the combined output, or empty list if no output was captured
     * @see #getAll()
     * @see #getOutLines()
     * @see #getErrLines()
     * @see #getChronologicalLines()
     */
    public List<String> getAllLines() {
        var content = getAll();
        if (content.isEmpty()) {
            return List.of();
        }
        return content.lines().toList();
    }

    /**
     * Retrieves all output content in chronological order as a single string.
     * <p>
     * This method combines all captured output (both stdout and stderr) in the
     * exact order it was written during test execution. This preserves the
     * interleaving of stdout and stderr as it would appear in a real console.
     * The content of each entry is concatenated verbatim without inserting any
     * synthetic line separators, so {@code print()} calls without newlines are
     * preserved exactly as written.
     *
     * @return the chronologically ordered output content, or empty string if no output was captured
     * @see #getChronologicalEntries()
     * @see #getAll()
     */
    public String getChronologicalContent() {
        var builder = new StringBuilder();
        for (var entry : chronologicalEntries) {
            builder.append(entry.content());
        }
        return builder.toString();
    }

    /**
     * Retrieves all output entries in chronological order.
     * <p>
     * This method returns an immutable list of all output entries, preserving
     * the exact order in which stdout and stderr output occurred during test
     * execution. Each entry includes the output type, content, and timestamp.
     *
     * @return an immutable list of output entries in chronological order
     * @see OutputEntry
     * @see #getChronologicalContent()
     */
    public List<OutputEntry> getChronologicalEntries() {
        return Collections.unmodifiableList(chronologicalEntries);
    }

    /**
     * Retrieves all output content in chronological order as a list of lines.
     * <p>
     * This method splits the chronologically ordered output into individual
     * lines using Java's built-in line processing. Empty lines are preserved
     * in the result. The line splitting handles various line separator formats
     * (\n, \r\n, \r) correctly across different platforms.
     *
     * @return a list of lines from the chronologically ordered output, or an empty list if no output was captured
     * @see #getChronologicalContent()
     * @see #getAllLines()
     */
    public List<String> getChronologicalLines() {
        var content = getChronologicalContent();
        if (content.isEmpty()) {
            return List.of();
        }
        return content.lines().toList();
    }

    /**
     * Retrieves the captured stderr content as a string.
     * <p>
     * This method converts all data written to {@code System.err} during the test
     * execution into a string decoded as UTF-8. Line separators are preserved as
     * they were originally written.
     *
     * @return the complete stderr content as a string, or empty string if no stderr was captured
     * @see #getOut()
     * @see #getAll()
     */
    public String getErr() {
        return stderr.toString(StandardCharsets.UTF_8);
    }

    /**
     * Retrieves the captured stderr content as a raw byte array.
     * <p>
     * This method provides access to the raw bytes that were written to stderr,
     * without any character encoding conversion. This can be useful for analyzing
     * binary data or when specific encoding handling is required.
     *
     * @return a copy of the stderr byte data, or empty array if no stderr was captured
     * @see #getErr()
     * @see #getOutAsBytes()
     */
    public byte[] getErrAsBytes() {
        return stderr.toByteArray();
    }

    /**
     * Retrieves the captured stderr content as a list of lines.
     * <p>
     * This method splits the stderr output into individual lines using
     * Java's built-in line processing. Empty lines are preserved in the result.
     * The line splitting handles various line separator formats (\n, \r\n, \r)
     * correctly across different platforms.
     *
     * @return a list of lines from stderr, or empty list if no stderr was captured
     * @see #getErr()
     * @see #getOutLines()
     * @see #getAllLines()
     */
    public List<String> getErrLines() {
        var content = getErr();
        if (content.isEmpty()) {
            return List.of();
        }
        return content.lines().toList();
    }

    /**
     * Retrieves the captured stdout content as a string.
     * <p>
     * This method converts all data written to {@code System.out} during the test
     * execution into a string decoded as UTF-8. Line separators are preserved as
     * they were originally written.
     *
     * @return the complete stdout content as a string, or empty string if no stdout was captured
     * @see #getErr()
     * @see #getAll()
     */
    public String getOut() {
        return stdout.toString(StandardCharsets.UTF_8);
    }

    /**
     * Retrieves the captured stdout content as a raw byte array.
     * <p>
     * This method provides access to the raw bytes that were written to stdout,
     * without any character encoding conversion. This can be useful for analyzing
     * binary data or when specific encoding handling is required.
     *
     * @return a copy of the stdout byte data, or empty array if no stdout was captured
     * @see #getOut()
     * @see #getErrAsBytes()
     */
    public byte[] getOutAsBytes() {
        return stdout.toByteArray();
    }

    /**
     * Retrieves the captured stdout content as a list of lines.
     * <p>
     * This method splits the stdout output into individual lines using
     * Java's built-in line processing. Empty lines are preserved in the result.
     * The line splitting handles various line separator formats (\n, \r\n, \r)
     * correctly across different platforms.
     *
     * @return a list of lines from stdout, or empty list if no stdout was captured
     * @see #getOut()
     * @see #getErrLines()
     * @see #getAllLines()
     */
    public List<String> getOutLines() {
        var content = getOut();
        if (content.isEmpty()) {
            return List.of();
        }
        return content.lines().toList();
    }

    /**
     * Determines whether no output was captured to either stdout or stderr
     * <p>
     * This method returns {@code true} only when both stdout and stderr
     * are completely empty (zero bytes captured). It's useful for verifying
     * that a test method produces no console output.
     *
     * @return {@code true} if both stdout and stderr are empty, {@code false} otherwise
     * @see #getOut()
     * @see #getErr()
     */
    public boolean isEmpty() {
        return stdout.size() == 0 && stderr.size() == 0;
    }

    /**
     * Searches for the specified text within the captured stdout content.
     * <p>
     * This method performs a case-sensitive substring search within the
     * stdout content. It's equivalent to calling {@code getOut().contains(text)}
     * but provides a more readable API for common assertions.
     *
     * @param text the text to search for in stdout (must not be {@code null})
     * @return {@code true} if stdout contains the specified text, {@code false} otherwise
     * @throws NullPointerException if text is {@code null}
     * @see #errContains(String)
     * @see #contains(String)
     * @see String#contains(CharSequence)
     */
    public boolean outContains(String text) {
        return getOut().contains(text);
    }

    /**
     * Adds an output entry to the chronological list.
     * <p>
     * This method is used internally by the capture mechanism to record output
     * events with their timestamps and types in chronological order.
     *
     * @param type    the type of output (STDOUT or STDERR)
     * @param content the content that was written
     */
    void addEntry(OutputType type, String content) {
        chronologicalEntries.add(new OutputEntry(type, content, Instant.now()));
    }

    /**
     * Enumeration of output types for distinguishing between stdout and stderr.
     * <p>
     * This enum is used to identify whether a particular piece of output was
     * written to stdout or stderr. It's used in conjunction with {@link OutputEntry}
     * to provide chronological output tracking.
     *
     * @see OutputEntry
     * @see CapturedOutput#getChronologicalEntries()
     * @since 1.0
     */
    public enum OutputType {
        /**
         * Represents output written to stdout (System.out).
         */
        STDOUT,

        /**
         * Represents output written to stderr (System.err).
         */
        STDERR
    }

    /**
     * Represents a single output entry with type, content, and timestamp.
     * <p>
     * This record encapsulates a single piece of output that was written to either
     * stdout or stderr during test execution. Each entry includes the output type
     * (STDOUT or STDERR), the actual content, and the timestamp when it occurred.
     * <p>
     * Instances of this record are immutable and are created automatically by the
     * capture mechanism when output occurs.
     *
     * @param type      the {@link OutputType type} of output
     * @param content   the content that was written
     * @param timestamp the timestamp when the event occurred
     * @see OutputType
     * @see CapturedOutput#getChronologicalEntries()
     * @since 1.0
     */
    public record OutputEntry(OutputType type, String content, Instant timestamp) {

        /**
         * Determines if this output entry is from stderr.
         *
         * @return {@code true} if this entry is from stderr, {@code false} otherwise
         */
        public boolean isStderr() {
            return type == OutputType.STDERR;
        }

        /**
         * Determines if this output entry is from stdout.
         *
         * @return {@code true} if this entry is from stdout, {@code false} otherwise
         */
        public boolean isStdout() {
            return type == OutputType.STDOUT;
        }
    }
}