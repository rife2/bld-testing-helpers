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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation to capture stdout and stderr output for specific test methods.
 * <p>
 * This annotation can be applied to individual test methods or entire test classes
 * to capture all console output (both {@code System.out} and {@code System.err})
 * during test execution. The captured output is then made available through a
 * {@link CapturedOutput} parameter that can be injected into the test method.
 * <p>
 * The annotation automatically handles the setup and teardown of output
 * capture, ensuring that the original stdout and stderr streams are properly
 * restored after each test execution. Additionally, it provides chronological
 * tracking of output with timestamps and type information.
 *
 * <h3>Basic Usage Example:</h3>
 *
 * <blockquote><pre>
 * class MyTest {
 *     &#64;Test
 *     &#64;CaptureOutput
 *     void testConsoleOutput(CapturedOutput output) {
 *         System.out.println("Hello World");
 *         System.err.println("Error message");
 *
 *         // Traditional stream-based access
 *         assertEquals("Hello World\n", output.getOut());
 *         assertEquals("Error message\n", output.getErr());
 *         assertTrue(output.contains("Hello"));
 *     }
 *
 *     &#64;Test
 *     void regularTest() {
 *         // This test runs normally without output capture
 *         System.out.println("Goes to console");
 *     }
 * }</pre></blockquote>
 *
 * <h3>Chronological Tracking Example:</h3>
 *
 * <blockquote><pre>
 * &#64;Test
 * &#64;CaptureOutput
 * void testChronologicalOutput(CapturedOutput output) {
 *     System.out.print("First stdout");
 *     System.err.print("First stderr");
 *     System.out.println(" - Second stdout");
 *
 *     // Access output in exact chronological order
 *     String chronological = output.getChronologicalContent();
 *     assertEquals("First stdoutFirst stderr - Second stdout\n", chronological);
 *
 *     // Access individual entries with timestamps and types
 *     List&lt;CapturedOutput.OutputEntry&gt; entries = output.getChronologicalEntries();
 *     assertEquals(3, entries.size());
 *     assertEquals(CapturedOutput.OutputType.STDOUT, entries.get(0).getType());
 *     assertEquals("First stdout", entries.get(0).getContent());
 *     assertTrue(entries.get(0).getTimestamp().isBefore(entries.get(1).getTimestamp()));
 * }</pre></blockquote>
 *
 * <h3>Class-Level Usage:</h3>
 *
 * <blockquote><pre>
 * &#64;CaptureOutput
 * class MyTestClass {
 *     &#64;Test
 *     void testOne(CapturedOutput output) {
 *         // This test has output capture enabled
 *         System.out.println("Test one output");
 *         assertFalse(output.isEmpty());
 *     }
 *
 *     &#64;Test
 *     void testTwo(CapturedOutput output) {
 *         // This test also has output capture enabled
 *         System.err.println("Test two error");
 *         assertTrue(output.errContains("error"));
 *     }
 * }</pre></blockquote>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *     <li><strong>Targeted capture</strong> - Only applies to annotated test methods or classes</li>
 *     <li><strong>Automatic cleanup</strong> - Restores original streams after test execution</li>
 *     <li><strong>Parameter injection</strong> - Provides {@link CapturedOutput} for analysis</li>
 *     <li><strong>Thread-safe</strong> - Each test gets its own capture instance</li>
 *     <li><strong>Chronological tracking</strong> - Records output order, timestamps, and stream types</li>
 *     <li><strong>Multiple access patterns</strong> - Stream-grouped or chronologically ordered access</li>
 * </ul>
 *
 * <h3>Output Access Methods:</h3>
 * <ul>
 *     <li><strong>Stream-grouped access:</strong>
 *         <ul>
 *             <li>{@link CapturedOutput#getOut()} - All stdout content</li>
 *             <li>{@link CapturedOutput#getErr()} - All stderr content</li>
 *             <li>{@link CapturedOutput#getAll()} - Stdout followed by stderr</li>
 *         </ul>
 *     </li>
 *     <li><strong>Chronological access:</strong>
 *         <ul>
 *             <li>{@link CapturedOutput#getChronologicalContent()} - Output in exact order of occurrence</li>
 *             <li>{@link CapturedOutput#getChronologicalEntries()} - Individual entries with metadata</li>
 *             <li>{@link CapturedOutput#getChronologicalLines()} - Lines in chronological order</li>
 *         </ul>
 *     </li>
 *     <li><strong>Line-based access:</strong>
 *         <ul>
 *             <li>{@link CapturedOutput#getOutLines()} - Stdout lines</li>
 *             <li>{@link CapturedOutput#getErrLines()} - Stderr lines</li>
 *             <li>{@link CapturedOutput#getAllLines()} - All lines (stdout first, then stderr)</li>
 *         </ul>
 *     </li>
 *     <li><strong>Raw byte access:</strong>
 *         <ul>
 *             <li>{@link CapturedOutput#getOutAsBytes()} - Raw stdout bytes</li>
 *             <li>{@link CapturedOutput#getErrAsBytes()} - Raw stderr bytes</li>
 *         </ul>
 *     </li>
 *     <li><strong>Search methods:</strong>
 *         <ul>
 *             <li>{@link CapturedOutput#contains(String)} - Search in both streams</li>
 *             <li>{@link CapturedOutput#outContains(String)} - Search in stdout only</li>
 *             <li>{@link CapturedOutput#errContains(String)} - Search in stderr only</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h3>Chronological vs. Stream-Grouped Output:</h3>
 * <p>
 * The annotation provides two different ways to access captured output:
 * </p>
 * <ul>
 *     <li><strong>Stream-grouped:</strong> where all stdout content is grouped together,
 *         followed by all stderr content.</li>
 *     <li><strong>Chronological:</strong> where the exact order in which output
 *         occurred is preserved, interleaving stdout and stderr as it would appear in a real console.
 *         Each output event includes timestamp and stream type information.</li>
 * </ul>
 *
 * <h3>Performance Considerations:</h3>
 * <ul>
 *     <li>Chronological tracking adds minimal overhead per output operation</li>
 *     <li>Each output operation (print, println, printf) creates one chronological entry</li>
 *     <li>Timestamps are captured using {@link java.time.Instant#now()}</li>
 *     <li>Memory usage scales linearly with the number of output operations</li>
 * </ul>
 *
 * <h3>Limitations:</h3>
 * <ul>
 *     <li>Does not capture output from native code or processes spawned by the JVM</li>
 *     <li>Console input (System.in) is not captured, only output streams</li>
 *     <li>Direct writes to file descriptors bypass the capture mechanism</li>
 *     <li>Chronological tracking granularity is per print/write operation, not per character</li>
 * </ul>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see CapturedOutput
 * @see CaptureOutputExtension
 * @see CapturedOutput.OutputEntry
 * @see CapturedOutput.OutputType
 * @since 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CaptureOutputExtension.class)
public @interface CaptureOutput {

}