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

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.SystemPrintln", "PMD.TestClassWithoutTestCases"})
class CapturedOutputTest {

    private static final String EOL = System.lineSeparator();

    @Test
    void bothStreams() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("Stdout output\n".getBytes());
        err.writeBytes("Stderr output\n".getBytes());
        var captured = new CapturedOutput(out, err);

        assertFalse(captured.isEmpty());
        assertEquals("Stdout output\n", captured.getOut());
        assertEquals("Stderr output\n", captured.getErr());
        assertEquals("Stdout output\nStderr output\n", captured.getAll());
        assertTrue(captured.outContains("Stdout"));
        assertTrue(captured.errContains("Stderr"));
        assertTrue(captured.contains("output"));
        assertTrue(captured.contains("Stdout"));
        assertTrue(captured.contains("Stderr"));
        assertFalse(captured.contains("NotThere"));
    }

    @Test
    void checkToStringFormat() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("A\n".getBytes());
        err.writeBytes("B\n".getBytes());
        var captured = new CapturedOutput(out, err);

        var expected = """
                CapturedOutput{stdout='A
                ', stderr='B
                ', chronologicalEntries=0 entries}""";
        assertEquals(expected, captured.toString());
    }

    @Test
    @CaptureOutput
    void checkToStringFormatForChronologicalEntries(CapturedOutput output) {
        System.out.println("Stdout line 1");
        System.err.println("Stderr line 1");

        var entries = output.getChronologicalEntries();

        var expected = "OutputEntry[type=STDOUT, content=Stdout line 1" + EOL +
                ", timestamp=" + entries.get(0).timestamp() + ']';
        assertEquals(expected, entries.get(0).toString());

        expected = "OutputEntry[type=STDERR, content=Stderr line 1" + EOL +
                ", timestamp=" + entries.get(1).timestamp() + ']';
        assertEquals(expected, entries.get(1).toString());
    }

    @Test
    void emptyOutput() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        var captured = new CapturedOutput(out, err);

        assertTrue(captured.isEmpty());
        assertEquals("", captured.getOut());
        assertEquals("", captured.getErr());
        assertEquals("", captured.getAll());
        assertArrayEquals(new byte[0], captured.getOutAsBytes());
        assertArrayEquals(new byte[0], captured.getErrAsBytes());
        assertFalse(captured.contains("anything"));
        assertFalse(captured.outContains("foo"));
        assertFalse(captured.errContains("bar"));
    }

    @Test
    void nullArgumentChecks() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("x".getBytes());
        err.writeBytes("y".getBytes());
        var captured = new CapturedOutput(out, err);

        assertThrows(NullPointerException.class, () -> captured.outContains(null));
        assertThrows(NullPointerException.class, () -> captured.errContains(null));
        assertThrows(NullPointerException.class, () -> captured.contains(null));
    }

    @Test
    void stderrOnly() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        err.writeBytes("Error message\n".getBytes());
        var captured = new CapturedOutput(out, err);

        assertFalse(captured.isEmpty());
        assertEquals("", captured.getOut());
        assertEquals("Error message\n", captured.getErr());
        assertEquals("Error message\n", captured.getAll());
        assertFalse(captured.outContains("Hello"));
        assertTrue(captured.errContains("Error"));
        assertTrue(captured.contains("Error"));
        assertFalse(captured.contains("Hello"));
        assertArrayEquals(new byte[0], captured.getOutAsBytes());
        assertArrayEquals("Error message\n".getBytes(), captured.getErrAsBytes());
    }

    @Test
    void stdoutOnly() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        out.writeBytes("Hello World\n".getBytes());
        var captured = new CapturedOutput(out, err);

        assertFalse(captured.isEmpty());
        assertEquals("Hello World\n", captured.getOut());
        assertEquals("", captured.getErr());
        assertEquals("Hello World\n", captured.getAll());
        assertTrue(captured.outContains("Hello"));
        assertFalse(captured.errContains("Error"));
        assertTrue(captured.contains("World"));
        assertFalse(captured.contains("Error"));
        assertArrayEquals("Hello World\n".getBytes(), captured.getOutAsBytes());
        assertArrayEquals(new byte[0], captured.getErrAsBytes());
    }

    @Nested
    @DisplayName("Chronological Output Capture Tests")
    class CapturedOutputExampleTest {

        @Test
        @CaptureOutput
        void chronologicalCapture(CapturedOutput output) {
            System.out.print("First stdout");
            System.err.print("First stderr");
            System.out.println(" - Second stdout");
            System.err.println(" - Second stderr");

            // Get chronological entries
            var entries = output.getChronologicalEntries();
            assertEquals(4, entries.size());

            // Verify order and content
            assertEquals(CapturedOutput.OutputType.STDOUT, entries.get(0).type());
            assertEquals("First stdout", entries.get(0).content());
            assertTrue(entries.get(0).isStdout());
            assertFalse(entries.get(0).isStderr());

            assertEquals(CapturedOutput.OutputType.STDERR, entries.get(1).type());
            assertEquals("First stderr", entries.get(1).content());
            assertTrue(entries.get(1).isStderr());
            assertFalse(entries.get(1).isStdout());

            assertEquals(CapturedOutput.OutputType.STDOUT, entries.get(2).type());
            assertEquals(" - Second stdout" + EOL, entries.get(2).content());

            assertEquals(CapturedOutput.OutputType.STDERR, entries.get(3).type());
            assertEquals(" - Second stderr" + EOL, entries.get(3).content());

            // Verify timestamps are in order
            for (int i = 1; i < entries.size(); i++) {
                assertTrue(entries.get(i - 1).timestamp().isBefore(entries.get(i).timestamp()) ||
                                entries.get(i - 1).timestamp().equals(entries.get(i).timestamp()),
                        "Timestamps should be in chronological order");
            }
        }

        @Test
        @CaptureOutput
        void chronologicalContent(CapturedOutput output) {
            System.out.print("A");
            System.err.print("B");
            System.out.print("C");
            System.err.print("D");

            // Chronological content preserves exact order
            assertEquals("ABCD", output.getChronologicalContent());

            // Traditional getAll() groups by stream type
            assertEquals("AC", output.getOut()); // Only stdout
            assertEquals("BD", output.getErr());  // Only stderr
            assertEquals("ACBD", output.getAll()); // stdout then stderr
        }

        @Test
        @CaptureOutput
        void chronologicalContentWithEmptyLines(CapturedOutput output) {
            var entries = output.getChronologicalLines();
            assertTrue(entries.isEmpty());
        }

        @Test
        @CaptureOutput
        void chronologicalLines(CapturedOutput output) {
            System.out.println("Stdout line 1");
            System.err.println("Stderr line 1");
            System.out.println("Stdout line 2");

            var chronologicalLines = output.getChronologicalLines();
            var expectedLines = List.of("Stdout line 1", "Stderr line 1", "Stdout line 2");
            assertEquals(expectedLines, chronologicalLines);

            // Compare with traditional line methods
            var allLines = output.getAllLines(); // Groups stdout first, then stderr
            var expectedAllLines = List.of("Stdout line 1", "Stdout line 2", "Stderr line 1");
            assertEquals(expectedAllLines, allLines);
        }

        @Test
        @CaptureOutput
        void differentDataTypesWithAppend(CapturedOutput output) {
            System.out.append("String: ");
            System.out.append("text");
            System.out.append("CharSequence: ");
            System.out.append(new StringBuilder("abc"));
            System.out.append("Char: ");
            System.out.append('c');
            System.out.append("CharSequence: (start, end): ");
            System.out.append(new StringBuilder("abc"), 1, 2);
            System.out.append(null, 1, 3);

            var entries = output.getChronologicalEntries();
            assertEquals(9, entries.size());

            assertEquals("String: ", entries.get(0).content());
            assertEquals("text", entries.get(1).content());
            assertEquals("CharSequence: ", entries.get(2).content());
            assertEquals("abc", entries.get(3).content());
            assertEquals("Char: ", entries.get(4).content());
            assertEquals("c", entries.get(5).content());
            assertEquals("CharSequence: (start, end): ", entries.get(6).content());
            assertEquals("b", entries.get(7).content());
            assertEquals("null", entries.get(8).content());
        }

        @Test
        @CaptureOutput
        void differentDataTypesWithPrint(CapturedOutput output) {
            System.out.print("String: ");
            System.out.print("text");
            System.out.print("Integer: ");
            System.out.print(42);
            System.out.print("Boolean: ");
            System.out.print(true);
            System.out.print("Double: ");
            System.out.print(3.14);
            System.out.print("Char: ");
            System.out.print('c');
            System.out.print("Long: ");
            System.out.print(10L);
            System.out.print("Float: ");
            System.out.print(1.1f);
            System.out.print("Char[]: ");
            System.out.print(new char[]{'a', 'b', 'c'});
            System.out.print("Object: ");
            System.out.print((Object) 5);
            System.out.print((String) null);

            var entries = output.getChronologicalEntries();
            assertEquals(18, entries.size());

            // Verify content types are captured as strings
            assertEquals("String: ", entries.get(0).content());
            assertEquals("text", entries.get(1).content());
            assertEquals("Integer: ", entries.get(2).content());
            assertEquals("42", entries.get(3).content());
            assertEquals("Boolean: ", entries.get(4).content());
            assertEquals("true", entries.get(5).content());
            assertEquals("Double: ", entries.get(6).content());
            assertEquals("3.14", entries.get(7).content());
            assertEquals("Char: ", entries.get(8).content());
            assertEquals("c", entries.get(9).content());
            assertEquals("Long: ", entries.get(10).content());
            assertEquals("10", entries.get(11).content());
            assertEquals("Float: ", entries.get(12).content());
            assertEquals("1.1", entries.get(13).content());
            assertEquals("Char[]: ", entries.get(14).content());
            assertEquals("abc", entries.get(15).content());
            assertEquals("Object: ", entries.get(16).content());
            assertEquals("5", entries.get(17).content());
        }

        @Test
        @CaptureOutput
        void differentDataTypesWithPrintLn(CapturedOutput output) {
            System.out.print("String: ");
            System.out.println("text");
            System.out.print("Integer: ");
            System.out.println(42);
            System.out.print("Boolean: ");
            System.out.println(true);
            System.out.print("Double: ");
            System.out.println(3.14);
            System.out.print("Char: ");
            System.out.println('c');
            System.out.print("Long: ");
            System.out.println(10L);
            System.out.print("Float: ");
            System.out.println(1.1f);
            System.out.print("Char[]: ");
            System.out.println(new char[]{'a', 'b', 'c'});
            System.out.print("Object: ");
            System.out.println((Object) 5);
            System.out.println();

            var entries = output.getChronologicalEntries();
            assertEquals(19, entries.size());

            // Verify content types are captured as strings
            assertEquals("String: ", entries.get(0).content());
            assertEquals("text" + EOL, entries.get(1).content());
            assertEquals("Integer: ", entries.get(2).content());
            assertEquals("42" + EOL, entries.get(3).content());
            assertEquals("Boolean: ", entries.get(4).content());
            assertEquals("true" + EOL, entries.get(5).content());
            assertEquals("Double: ", entries.get(6).content());
            assertEquals("3.14" + EOL, entries.get(7).content());
            assertEquals("Char: ", entries.get(8).content());
            assertEquals("c" + EOL, entries.get(9).content());
            assertEquals("Long: ", entries.get(10).content());
            assertEquals("10" + EOL, entries.get(11).content());
            assertEquals("Float: ", entries.get(12).content());
            assertEquals("1.1" + EOL, entries.get(13).content());
            assertEquals("Char[]: ", entries.get(14).content());
            assertEquals("abc" + EOL, entries.get(15).content());
            assertEquals("Object: ", entries.get(16).content());
            assertEquals("5" + EOL, entries.get(17).content());
            assertEquals(EOL, entries.get(18).content());
        }

        @Test
        @CaptureOutput
        void printfWithFormat(CapturedOutput output) {
            System.out.printf("%s %.2f", 5, 3.14);

            var entries = output.getChronologicalEntries();

            assertEquals("5 3.14", entries.get(0).content());
        }

        @Test
        @CaptureOutput
        void printfWithLocaleAndFormat(CapturedOutput output) {
            System.out.printf(Locale.FRANCE, "%+10.4f", Math.E);

            var entries = output.getChronologicalEntries();
            assertEquals("   +2,7183", entries.get(0).content());
        }
    }

    @Nested
    @CaptureOutput
    @DisplayName("Class-Level Output Capture Tests")
    class ClassLevelCaptureOutputTests {

        /**
         * Counter to track test execution order for demonstration purposes.
         */
        @SuppressWarnings("PMD.RedundantFieldInitializer")
        private static int testCounter = 0;

        @AfterEach
        void afterEach(CapturedOutput output) {
            System.out.println("Cleaning up test #" + testCounter);

            // Verify that cleanup output is captured
            assertTrue(output.outContains("Cleaning up test #" + testCounter));
        }

        @BeforeEach
        void beforeEach(CapturedOutput output) {
            testCounter++;
            System.out.println("Setting up test #" + testCounter);

            // Verify that setup output is captured
            assertTrue(output.outContains("Setting up test #" + testCounter));
        }

        @Test
        @DisplayName("Should provide byte-level access")
        void byteAccess(CapturedOutput output) {
            var stdoutMessage = "Stdout bytes test";
            var stderrMessage = "Stderr bytes test";

            System.out.print(stdoutMessage);
            System.err.print(stderrMessage);

            var stdoutBytes = output.getOutAsBytes();
            var stderrBytes = output.getErrAsBytes();

            var stdoutContent = new String(stdoutBytes);
            var stderrContent = new String(stderrBytes);

            assertTrue(stdoutContent.contains(stdoutMessage));
            assertTrue(stderrContent.contains(stderrMessage));
        }

        @Test
        @DisplayName("Should track different data types chronologically")
        void chronologicalDataTypes(CapturedOutput output) {
            System.out.print("String: ");
            System.out.println("text");
            System.out.print("Integer: ");
            System.out.println(42);
            System.err.print("Boolean: ");
            System.err.println(true);
            System.out.print("Double: ");
            System.out.println(3.14);

            var entries = output.getChronologicalEntries();

            // Filter out setup entries for cleaner testing
            var dataEntries = entries.stream()
                    .filter(entry -> !entry.content().contains("Setting up test"))
                    .toList();

            assertEquals(8, dataEntries.size());

            // Verify content and types
            assertEquals("String: ", dataEntries.get(0).content());
            assertEquals(CapturedOutput.OutputType.STDOUT, dataEntries.get(0).type());

            assertEquals("text" + EOL, dataEntries.get(1).content());
            assertEquals(CapturedOutput.OutputType.STDOUT, dataEntries.get(1).type());

            assertEquals("Integer: ", dataEntries.get(2).content());
            assertEquals(CapturedOutput.OutputType.STDOUT, dataEntries.get(2).type());

            assertEquals("42" + EOL, dataEntries.get(3).content());
            assertEquals(CapturedOutput.OutputType.STDOUT, dataEntries.get(3).type());

            assertEquals("Boolean: ", dataEntries.get(4).content());
            assertEquals(CapturedOutput.OutputType.STDERR, dataEntries.get(4).type());

            assertEquals("true" + EOL, dataEntries.get(5).content());
            assertEquals(CapturedOutput.OutputType.STDERR, dataEntries.get(5).type());

            // Verify timestamps are in chronological order
            for (int i = 1; i < dataEntries.size(); i++) {
                assertTrue(
                        dataEntries.get(i - 1).timestamp().isBefore(dataEntries.get(i).timestamp()) ||
                                dataEntries.get(i - 1).timestamp().equals(dataEntries.get(i).timestamp()),
                        "Timestamps should be in chronological order"
                );
            }
        }

        @Test
        @DisplayName("Should handle line-based output analysis")
        void lineBasedAnalysis(CapturedOutput output) {
            System.out.println("Stdout line 1");
            System.out.println("Stdout line 2");
            System.err.println("Stderr line 1");
            System.out.println("Stdout line 3");

            // Test individual stream-lines
            var stdoutLines = output.getOutLines();
            var stderrLines = output.getErrLines();

            // Should contain setup line plus test lines
            assertTrue(stdoutLines.stream().anyMatch(line -> line.contains("Setting up test")));
            assertTrue(stdoutLines.contains("Stdout line 1"));
            assertTrue(stdoutLines.contains("Stdout line 2"));
            assertTrue(stdoutLines.contains("Stdout line 3"));

            assertTrue(stderrLines.contains("Stderr line 1"));

            // Test chronological lines
            var chronologicalLines = output.getChronologicalLines();
            assertTrue(chronologicalLines.contains("Stdout line 1"));
            assertTrue(chronologicalLines.contains("Stderr line 1"));
            assertTrue(chronologicalLines.contains("Stdout line 3"));

            // Find the index of our test lines in chronological order
            var line1Index = chronologicalLines.indexOf("Stdout line 1");
            var stderrIndex = chronologicalLines.indexOf("Stderr line 1");
            var line3Index = chronologicalLines.indexOf("Stdout line 3");

            assertTrue(line1Index >= 0 && stderrIndex >= 0 && line3Index >= 0);
            assertTrue(line1Index < stderrIndex);
            assertTrue(stderrIndex < line3Index);
        }

        @Test
        @DisplayName("Should handle tests with minimal output")
        void minimalOutput(CapturedOutput output) {
            // This test produces no additional output beyond setup

            // Should not be empty due to set up in output
            assertFalse(output.isEmpty());

            // Should contain setup output
            assertTrue(output.outContains("Setting up test #"));

            // Should have at least one chronological entry (from setup)
            assertFalse(output.getChronologicalEntries().isEmpty());
        }

        @Test
        @DisplayName("Should capture mixed stdout and stderr output")
        void mixedOutputCapture(CapturedOutput output) {
            System.out.println("First stdout message");
            System.err.println("First stderr message");
            System.out.println("Second stdout message");
            System.err.println("Second stderr message");

            // Verify both streams are captured
            assertTrue(output.outContains("First stdout message"));
            assertTrue(output.outContains("Second stdout message"));
            assertTrue(output.errContains("First stderr message"));
            assertTrue(output.errContains("Second stderr message"));

            // Verify chronological order
            var entries = output.getChronologicalEntries();
            var nonSetupEntries = entries.stream()
                    .filter(entry -> !entry.content().contains("Setting up test"))
                    .toList();

            assertEquals(4, nonSetupEntries.size());
            assertEquals(CapturedOutput.OutputType.STDOUT, nonSetupEntries.get(0).type());
            assertEquals(CapturedOutput.OutputType.STDERR, nonSetupEntries.get(1).type());
            assertEquals(CapturedOutput.OutputType.STDOUT, nonSetupEntries.get(2).type());
            assertEquals(CapturedOutput.OutputType.STDERR, nonSetupEntries.get(3).type());

            // Verify chronological content
            var chronological = output.getChronologicalContent();
            var expectedOrder = "First stdout message" + EOL
                    + "First stderr message" + EOL
                    + "Second stdout message" + EOL
                    + "Second stderr message" + EOL;
            assertTrue(chronological.contains(expectedOrder));
        }

        @Test
        @DisplayName("Should isolate output between test methods")
        void outputIsolation(CapturedOutput output) {
            var uniqueMessage = "Unique message for isolation test";
            System.out.println(uniqueMessage);

            // Should contain this test's output
            assertTrue(output.outContains(uniqueMessage));

            // Should NOT contain output from other test methods
            assertFalse(output.outContains("Hello from stdout test"));
            assertFalse(output.errContains("Error from stderr test"));
            assertFalse(output.outContains("First stdout message"));

            // But should still contain setup output from this test's BeforeEach
            assertTrue(output.outContains("Setting up test #"));
        }

        @Test
        @DisplayName("Should include setup and teardown output")
        void setupAndTeardownInclusion(CapturedOutput output) {
            System.out.println("Main test content");

            // At this point, setup has run but teardown hasn't yet
            assertTrue(output.outContains("Setting up test #"));
            assertTrue(output.outContains("Main test content"));

            // Teardown output will be captured after this method completes
            // but can be verified in other tests that run after this one

            var entries = output.getChronologicalEntries();
            var hasSetup = entries.stream().anyMatch(entry ->
                    entry.content().contains("Setting up test #"));
            var hasMainContent = entries.stream().anyMatch(entry ->
                    entry.content().contains("Main test content"));

            assertTrue(hasSetup, "Should contain setup output");
            assertTrue(hasMainContent, "Should contain main test content");
        }

        @Test
        @DisplayName("Should capture stderr output")
        void stderrCapture(CapturedOutput output) {
            var errorMessage = "Error from stderr test";
            System.err.println(errorMessage);
            System.err.print("Additional stderr content");

            // Verify stderr content
            assertTrue(output.errContains(errorMessage));
            assertTrue(output.errContains("Additional stderr content"));
            assertTrue(output.contains(errorMessage));

            // Verify chronological tracking
            var entries = output.getChronologicalEntries();
            assertTrue(entries.stream().anyMatch(entry ->
                    entry.type() == CapturedOutput.OutputType.STDERR &&
                            entry.content().contains(errorMessage)));
        }

        @Test
        @DisplayName("Should capture stdout output")
        void stdoutCapture(CapturedOutput output) {
            var message = "Hello from stdout test";
            System.out.println(message);
            System.out.print("Additional stdout content");

            // Verify stdout content
            assertTrue(output.outContains(message));
            assertTrue(output.outContains("Additional stdout content"));
            assertTrue(output.contains(message));

            // Verify chronological tracking
            var entries = output.getChronologicalEntries();
            assertTrue(entries.stream().anyMatch(entry ->
                    entry.type() == CapturedOutput.OutputType.STDOUT &&
                            entry.content().contains(message)));

            // Should contain setup output as well
            assertTrue(output.outContains("Setting up test #"));
        }
    }

    @Nested
    @DisplayName("Get Lines Tests")
    class GetLinesTests {

        @Test
        void allLinesWithBothStreams() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Stdout line 1\nStdout line 2\n".getBytes());
            err.writeBytes("Stderr line 1\nStderr line 2\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getAllLines();
            assertEquals(List.of("Stdout line 1", "Stdout line 2", "Stderr line 1", "Stderr line 2"), lines);
        }

        @Test
        void allLinesWithEmptyOutput() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            var captured = new CapturedOutput(out, err);

            var lines = captured.getAllLines();
            assertTrue(lines.isEmpty());
        }

        @Test
        void errLinesWithDifferentLineSeparators() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            // Test with different line separators: \n, \r\n, \r
            err.writeBytes("Error Unix\nError Windows\r\nError Mac\r".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertEquals(List.of("Error Unix", "Error Windows", "Error Mac"), lines);
        }

        @Test
        void errLinesWithEmptyOutput() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertTrue(lines.isEmpty());
        }

        @Test
        void errLinesWithMultipleLines() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            err.writeBytes("Error 1\nError 2\nError 3\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertEquals(List.of("Error 1", "Error 2", "Error 3"), lines);
        }

        @Test
        void errLinesWithSingleLine() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            err.writeBytes("Error message\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getErrLines();
            assertEquals(List.of("Error message"), lines);
        }

        @Test
        void linesWithTrailingNewlineHandling() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            // Content ending with a newline should not create an empty trailing line
            out.writeBytes("Line 1\nLine 2".getBytes()); // No trailing newline
            err.writeBytes("Error 1\nError 2\n".getBytes()); // With trailing newline
            var captured = new CapturedOutput(out, err);

            assertEquals(List.of("Line 1", "Line 2"), captured.getOutLines());
            assertEquals(List.of("Error 1", "Error 2"), captured.getErrLines());
            assertEquals(List.of("Line 1", "Line 2Error 1", "Error 2"), captured.getAllLines());
        }

        @Test
        void outLinesWithDifferentLineSeparators() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            // Test with different line separators: \n, \r\n, \r
            out.writeBytes("Unix\nWindows\r\nClassic Mac\r".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Unix", "Windows", "Classic Mac"), lines);
        }

        @Test
        void outLinesWithEmptyLines() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Line 1\n\nLine 3\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Line 1", "", "Line 3"), lines);
        }

        @Test
        void outLinesWithEmptyOutput() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertTrue(lines.isEmpty());
        }

        @Test
        void outLinesWithMultipleLines() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Line 1\nLine 2\nLine 3\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Line 1", "Line 2", "Line 3"), lines);
        }

        @Test
        void outLinesWithSingleLine() {
            var out = new java.io.ByteArrayOutputStream();
            var err = new java.io.ByteArrayOutputStream();
            out.writeBytes("Hello World\n".getBytes());
            var captured = new CapturedOutput(out, err);

            var lines = captured.getOutLines();
            assertEquals(List.of("Hello World"), lines);
        }
    }
}