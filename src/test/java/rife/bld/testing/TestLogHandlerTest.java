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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete unit tests for optimized {@link TestLogHandler}
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TestClassWithoutTestCases"})
class TestLogHandlerTest {

    // Reusable LogRecord instances to avoid object creation in loops
    private static final LogRecord INFO_RECORD_1 = new LogRecord(Level.INFO, "Message 1");
    private static final LogRecord INFO_RECORD_HELLO = new LogRecord(Level.INFO, "Hello world");
    private static final LogRecord INFO_RECORD_HELLO_AGAIN = new LogRecord(Level.INFO, "Hello again");
    // Reusable null message record
    private static final LogRecord NULL_INFO_RECORD = new LogRecord(Level.INFO, null);
    private static final LogRecord SEVERE_RECORD_ERROR = new LogRecord(Level.SEVERE, "Error occurred");
    private static final LogRecord WARNING_RECORD_2 = new LogRecord(Level.WARNING, "Message 2");
    private static final LogRecord WARNING_RECORD_WARNING = new LogRecord(Level.WARNING, "Warning: something happened");
    private TestLogHandler handler;

    @BeforeEach
    void beforeEach() {
        handler = new TestLogHandler();
    }

    @Nested
    @DisplayName("Clear Functionality Tests")
    class ClearFunctionalityTests {

        @Test
        @DisplayName("Should clear all messages and records")
        void shouldClearAllMessagesAndRecords() {
            handler.publish(INFO_RECORD_1);
            handler.publish(WARNING_RECORD_2);

            assertEquals(2, handler.getLogMessages().size());
            assertEquals(2, handler.getLogRecords().size());

            handler.clear();

            assertTrue(handler.getLogMessages().isEmpty());
            assertTrue(handler.getLogRecords().isEmpty());
            assertFalse(handler.containsMessage("Message"));
            assertFalse(handler.hasLogLevel(Level.INFO));
            assertTrue(handler.isEmpty());
            assertEquals(0, handler.getRecordCount());
        }

        @Test
        @DisplayName("Should handle clear on empty handler")
        void shouldHandleClearOnEmptyHandler() {
            handler.clear();

            assertTrue(handler.getLogMessages().isEmpty());
            assertTrue(handler.getLogRecords().isEmpty());
            assertTrue(handler.isEmpty());
            assertEquals(0, handler.getRecordCount());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @ParameterizedTest
        @ValueSource(strings = {"Case", "Sensitive"})
        @DisplayName("Should find case-sensitive messages")
        void shouldFindCaseSensitiveMessages(String searchTerm) {
            var record = new LogRecord(Level.INFO, "Case Sensitive Message");
            handler.publish(record);
            assertTrue(handler.containsMessage(searchTerm));
        }

        @Test
        @DisplayName("Should handle special characters in messages")
        void shouldHandleSpecialCharactersInMessages() {
            var specialRecord = new LogRecord(Level.INFO, "Special: !@#$%^&*(){}[]|\\:;\"'<>,.?/~`");
            handler.publish(specialRecord);

            assertTrue(handler.containsMessage("Special:"));
            assertTrue(handler.containsMessage("!@#$"));
            assertTrue(handler.containsExactMessage("Special: !@#$%^&*(){}[]|\\:;\"'<>,.?/~`"));
        }

        @Test
        @DisplayName("Should handle very long message")
        void shouldHandleVeryLongMessage() {
            var longMessage = "Long message ".repeat(10000);
            var record = new LogRecord(Level.INFO, longMessage);

            handler.publish(record);

            assertTrue(handler.containsMessage("Long message"));
            assertEquals(1, handler.countMessagesContaining("Long"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"case", "sensitive"})
        @DisplayName("Should not find lowercase in case-sensitive search")
        void shouldNotFindLowercaseInCaseSensitiveSearch(String searchTerm) {
            var record = new LogRecord(Level.INFO, "Case Sensitive Message");
            handler.publish(record);
            assertFalse(handler.containsMessage(searchTerm));
        }
    }

    @Nested
    @DisplayName("Enhanced Record Access Tests")
    class EnhancedRecordAccessTests {

        @Test
        @DisplayName("Should correctly identify empty state")
        void shouldCorrectlyIdentifyEmptyState() {
            assertTrue(handler.isEmpty());

            handler.publish(INFO_RECORD_1);
            assertFalse(handler.isEmpty());

            handler.clear();
            assertTrue(handler.isEmpty());
        }

        @Test
        @DisplayName("Should get correct record count")
        void shouldGetCorrectRecordCount() {
            assertEquals(0, handler.getRecordCount());

            handler.publish(INFO_RECORD_1);
            assertEquals(1, handler.getRecordCount());

            handler.publish(WARNING_RECORD_2);
            assertEquals(2, handler.getRecordCount());

            handler.clear();
            assertEquals(0, handler.getRecordCount());
        }

        @Test
        @DisplayName("Should get last record")
        void shouldGetLastRecord() {
            var firstRecord = new LogRecord(Level.INFO, "First message");
            var middleRecord = new LogRecord(Level.WARNING, "Middle message");
            var lastRecord = new LogRecord(Level.SEVERE, "Last message");

            handler.publish(firstRecord);
            handler.publish(middleRecord);
            handler.publish(lastRecord);

            var result = handler.getLastRecord();

            assertTrue(result.isPresent());
            assertEquals("Last message", result.get().getMessage());
            assertEquals(Level.SEVERE, result.get().getLevel());
        }

        @Test
        @DisplayName("Should get last record containing text")
        void shouldGetLastRecordContaining() {
            var firstHelloRecord = new LogRecord(Level.INFO, "First hello message");
            var differentRecord = new LogRecord(Level.WARNING, "Different message");
            var lastHelloRecord = new LogRecord(Level.SEVERE, "Last hello message");

            handler.publish(firstHelloRecord);
            handler.publish(differentRecord);
            handler.publish(lastHelloRecord);

            var result = handler.getLastRecordContaining("hello");

            assertTrue(result.isPresent());
            assertEquals("Last hello message", result.get().getMessage());
            assertEquals(Level.SEVERE, result.get().getLevel());
        }

        @Test
        @DisplayName("Should handle empty search in getLastRecordContaining")
        void shouldHandleEmptySearchInGetLastRecordContaining() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);
            assertEquals(Optional.empty(), handler.getLastRecordContaining(""));
        }

        @Test
        @DisplayName("Should return null for last record on empty handler")
        void shouldReturnNullForLastRecordOnEmptyHandler() {
            assertEquals(Optional.empty(), handler.getLastRecord());
        }

        @Test
        @DisplayName("Should return null when no record contains text")
        void shouldReturnNullWhenNoRecordContainsText() {
            var someRecord = new LogRecord(Level.INFO, "Some message");
            handler.publish(someRecord);
            assertEquals(Optional.empty(), handler.getLastRecordContaining("nonexistent"));
        }

        @ParameterizedTest(name = "Should get last record containing ''{0}''")
        @NullAndEmptySource
        @DisplayName("Should throw with null or empty messages")
        void shouldThrowWithNullOrEmptyMessage(String message) {
            var fooRecord = new LogRecord(Level.INFO, "foo");
            var nullRecord = new LogRecord(Level.INFO, message);
            var barRecord = new LogRecord(Level.FINE, "bar");

            handler.publish(fooRecord);
            handler.publish(nullRecord);
            handler.publish(barRecord);

            var result = handler.getLastRecordContaining("bar");

            assertTrue(result.isPresent());
            assertEquals("bar", result.get().getMessage());
            assertEquals(Level.FINE, result.get().getLevel());
        }

        @Test
        @DisplayName("Should throw with null search in getLastRecordContaining")
        @SuppressWarnings("DataFlowIssue")
        void shouldThrowWithNullSearchInGetLastRecordContaining() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);
            assertThrows(NullPointerException.class, () -> handler.getLastRecordContaining(null));
        }
    }

    @Nested
    @DisplayName("Handler Interface Tests")
    class HandlerInterfaceTests {

        static Stream<Runnable> handlerMethods() {
            return Stream.of(() -> new TestLogHandler().close(), () -> new TestLogHandler().flush());
        }

        @Test
        @DisplayName("Should handle flush operations")
        void shouldHandleFlushOperations() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            assertDoesNotThrow(() -> handler.flush());

            // Records should still be available after a flush
            assertEquals(1, handler.getRecordCount());
            assertTrue(handler.containsMessage("Test message"));
        }

        @ParameterizedTest
        @MethodSource("handlerMethods")
        @DisplayName("Should implement handler methods without exceptions")
        void shouldImplementHandlerMethodsWithoutExceptions(Runnable method) {
            assertDoesNotThrow(method::run);
        }
    }

    @Nested
    @DisplayName("Handler State Management Tests")
    class HandlerStateManagementTests {

        @Test
        @DisplayName("Should handle clear state correctly")
        void shouldHandleClearStateCorrectly() {
            assertFalse(handler.isClosed());

            var beforeCloseRecord = new LogRecord(Level.INFO, "Before close");
            handler.publish(beforeCloseRecord);
            assertEquals(1, handler.getRecordCount());

            handler.close();
            assertTrue(handler.isClosed());

            handler.clear();

            // Should not accept new records when closed
            var afterCloseRecord = new LogRecord(Level.WARNING, "After close");
            handler.publish(afterCloseRecord);
            assertEquals(0, handler.getRecordCount());
        }

        @Test
        @DisplayName("Should handle multiple close calls")
        void shouldHandleMultipleClearCalls() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            handler.close();
            assertTrue(handler.isClosed());

            // Multiple close calls should be safe
            assertDoesNotThrow(() -> handler.close());
            assertTrue(handler.isClosed());
        }

        @Test
        @DisplayName("Should respect log level filtering")
        void shouldRespectLogLevelFiltering() {
            // Set the handler level to WARNING and above
            handler.setLevel(Level.WARNING);

            var fineRecord = new LogRecord(Level.FINE, "Fine message");
            var infoRecord = new LogRecord(Level.INFO, "Info message");
            var warningRecord = new LogRecord(Level.WARNING, "Warning message");
            var severeRecord = new LogRecord(Level.SEVERE, "Severe message");

            handler.publish(fineRecord);     // Should be filtered
            handler.publish(infoRecord);     // Should be filtered
            handler.publish(warningRecord);  // Should be accepted
            handler.publish(severeRecord);   // Should be accepted

            assertEquals(2, handler.getRecordCount());
            assertTrue(handler.containsMessage("Warning message"));
            assertTrue(handler.containsMessage("Severe message"));
            assertFalse(handler.containsMessage("Fine message"));
            assertFalse(handler.containsMessage("Info message"));
        }
    }

    @Nested
    @DisplayName("Immutable Returns Tests")
    class ImmutableReturnsTests {

        @Test
        @DisplayName("Should provide snapshot of current state")
        void shouldProvideSnapshotOfCurrentState() {
            var initialRecord = new LogRecord(Level.INFO, "Initial message");
            handler.publish(initialRecord);

            List<String> messages1 = handler.getLogMessages();
            var records1 = handler.getLogRecords();

            // Add more records
            var additionalRecord = new LogRecord(Level.WARNING, "Additional message");
            handler.publish(additionalRecord);

            List<String> messages2 = handler.getLogMessages();
            var records2 = handler.getLogRecords();

            // The first snapshot should remain unchanged
            assertEquals(1, messages1.size());
            assertEquals(1, records1.size());

            // The second snapshot should reflect the current state
            assertEquals(2, messages2.size());
            assertEquals(2, records2.size());
        }

        @Test
        @DisplayName("Should return immutable log messages list")
        void shouldReturnImmutableLogMessagesList() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            List<String> messages = handler.getLogMessages();

            assertThrows(UnsupportedOperationException.class, () -> messages.add("Should not be added"));
            assertThrows(UnsupportedOperationException.class, messages::clear);
        }

        @Test
        @DisplayName("Should return immutable log records list")
        void shouldReturnImmutableLogRecordsList() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            var records = handler.getLogRecords();
            var shouldNotBeAddedRecord = new LogRecord(Level.WARNING, "Should not be added");

            assertThrows(UnsupportedOperationException.class, () -> records.add(shouldNotBeAddedRecord));
            assertThrows(UnsupportedOperationException.class, records::clear);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should maintain consistency across all operations")
        void shouldMaintainConsistencyAcrossAllOperations() {
            // Start with the empty handler
            assertTrue(handler.isEmpty());
            assertEquals(0, handler.getRecordCount());
            assertEquals(Optional.empty(), handler.getLastRecord());

            // Add the first record
            var record1 = new LogRecord(Level.INFO, "First message");
            handler.publish(record1);

            assertFalse(handler.isEmpty());
            assertEquals(1, handler.getRecordCount());
            assertTrue(handler.getLastRecord().isPresent());
            assertEquals(record1.getMessage(), handler.getLastRecord().get().getMessage());
            assertTrue(handler.containsMessage("First"));
            assertTrue(handler.containsExactMessage("First message"));
            assertEquals(1, handler.countMessagesContaining("First"));
            assertTrue(handler.getFirstRecordContaining("First").isPresent());
            assertEquals(record1.getMessage(), handler.getFirstRecordContaining("First").get().getMessage());
            assertTrue(handler.getLastRecordContaining("First").isPresent());
            assertEquals(record1.getMessage(), handler.getLastRecordContaining("First").get().getMessage());

            // Add the second record
            var record2 = new LogRecord(Level.WARNING, "Second message");
            handler.publish(record2);

            assertEquals(2, handler.getRecordCount());
            assertTrue(handler.getLastRecord().isPresent());
            assertEquals(record2.getMessage(), handler.getLastRecord().get().getMessage());
            assertTrue(handler.containsMessage("Second"));
            assertEquals(1, handler.countMessagesContaining("Second"));
            assertTrue(handler.getFirstRecordContaining("message").isPresent());
            assertEquals(record1.getMessage(), handler.getFirstRecordContaining("message").get().getMessage());
            assertTrue(handler.getLastRecordContaining("message").isPresent());
            assertEquals(record2.getMessage(), handler.getLastRecordContaining("message").get().getMessage());

            // Clear and verify
            handler.clear();

            assertTrue(handler.isEmpty());
            assertEquals(0, handler.getRecordCount());
            assertEquals(Optional.empty(), handler.getLastRecord());
            assertFalse(handler.containsMessage("First"));
            assertFalse(handler.containsMessage("Second"));
            assertEquals(0, handler.countMessagesContaining("message"));
            assertEquals(Optional.empty(), handler.getFirstRecordContaining("message"));
            assertEquals(Optional.empty(), handler.getLastRecordContaining("message"));
        }

        @Test
        @DisplayName("Should work with java.util.logging framework")
        void shouldWorkWithJavaUtilLoggingFramework() {
            var logger = Logger.getLogger("TestLogger");
            logger.addHandler(handler);
            logger.setLevel(Level.ALL);

            // Log some messages
            logger.info("Info message from logger");
            logger.warning("Warning message from logger");
            logger.severe("Severe message from logger");

            // Verify messages were captured
            assertEquals(3, handler.getRecordCount());
            assertTrue(handler.containsMessage("Info message from logger"));
            assertTrue(handler.containsMessage("Warning message from logger"));
            assertTrue(handler.containsMessage("Severe message from logger"));
            assertTrue(handler.hasLogLevel(Level.INFO));
            assertTrue(handler.hasLogLevel(Level.WARNING));
            assertTrue(handler.hasLogLevel(Level.SEVERE));

            // Clean up
            logger.removeHandler(handler);
        }
    }

    @Nested
    @DisplayName("Level-based Filtering Tests")
    class LevelBasedFilteringTests {

        private static final LogRecord CONFIG_RECORD = new LogRecord(Level.CONFIG, "Config message");
        private static final LogRecord FINER_RECORD = new LogRecord(Level.FINER, "Finer message");
        // Reusable records for level tests
        private static final LogRecord FINEST_RECORD = new LogRecord(Level.FINEST, "Finest message");
        private static final LogRecord FINE_RECORD = new LogRecord(Level.FINE, "Fine message");
        private static final LogRecord INFO_RECORD_1_LEVEL = new LogRecord(Level.INFO, "Info message 1");
        private static final LogRecord INFO_RECORD_2_LEVEL = new LogRecord(Level.INFO, "Info message 2");
        private static final LogRecord SEVERE_RECORD_LEVEL = new LogRecord(Level.SEVERE, "Severe message");
        private static final LogRecord WARNING_RECORD_LEVEL = new LogRecord(Level.WARNING, "Warning message");

        @BeforeEach
        void setUpLogRecords() {
            handler.publish(FINEST_RECORD);
            handler.publish(FINER_RECORD);
            handler.publish(FINE_RECORD);
            handler.publish(CONFIG_RECORD);
            handler.publish(INFO_RECORD_1_LEVEL);
            handler.publish(INFO_RECORD_2_LEVEL);
            handler.publish(WARNING_RECORD_LEVEL);
            handler.publish(SEVERE_RECORD_LEVEL);
        }

        @ParameterizedTest
        @CsvSource({
                "FINEST, 1",
                "FINER, 1",
                "FINE, 1",
                "CONFIG, 1",
                "INFO, 2",
                "WARNING, 1",
                "SEVERE, 1"
        })
        @DisplayName("Should count records at specific level")
        void shouldCountRecordsAtSpecificLevel(String levelName, long expectedCount) {
            var level = Level.parse(levelName);
            assertEquals(expectedCount, handler.countRecordsAtLevel(level));
        }

        @Test
        @DisplayName("Should count zero for non-existent level")
        void shouldCountZeroForNonExistentLevel() {
            assertEquals(0, handler.countRecordsAtLevel(Level.OFF));
        }

        @ParameterizedTest
        @CsvSource({"FINEST, 8",   // All records
                "FINE, 6",     // FINE and above
                "INFO, 4",     // INFO and above
                "WARNING, 2",  // WARNING and above
                "SEVERE, 1"    // Only SEVERE
        })
        @DisplayName("Should get records at or above level")
        void shouldGetRecordsAtOrAboveLevel(String levelName, int expectedCount) {
            var level = Level.parse(levelName);
            var records = handler.getRecordsAtOrAboveLevel(level);

            assertEquals(expectedCount, records.size());

            // Verify all returned records are at or above the specified level
            records.forEach(record -> assertTrue(record.getLevel().intValue() >= level.intValue()));
        }

        @Test
        @DisplayName("Should return immutable list from getRecordsAtOrAboveLevel")
        void shouldReturnImmutableListFromGetRecordsAtOrAboveLevel() {
            var records = handler.getRecordsAtOrAboveLevel(Level.INFO);
            var shouldNotBeAddedRecord = new LogRecord(Level.INFO, "Should not be added");

            assertThrows(UnsupportedOperationException.class, () -> records.add(shouldNotBeAddedRecord));
        }

        @Test
        @DisplayName("Should throw with null level in countRecordsAtLevel")
        @SuppressWarnings("DataFlowIssue")
        void shouldThrowWithNullLevelInCountRecordsAtLevel() {
            assertThrows(NullPointerException.class, () -> handler.countRecordsAtLevel(null));
        }

        @Test
        @DisplayName("Should throw with null level in getRecordsAtOrAboveLevel")
        @SuppressWarnings("DataFlowIssue")
        void shouldThrowWithNullLevelInGetRecordsAtOrAboveLevel() {
            assertThrows(NullPointerException.class, () -> handler.getRecordsAtOrAboveLevel(null));
        }
    }

    @Nested
    @DisplayName("Log Level Tests")
    class LogLevelTests {

        // Reusable test records
        private static final LogRecord INFO_TEST_RECORD = new LogRecord(Level.INFO, "Info message");
        private static final LogRecord SEVERE_TEST_RECORD = new LogRecord(Level.SEVERE, "Error message");
        private static final LogRecord WARNING_TEST_RECORD = new LogRecord(Level.WARNING, "Warning message");

        static Stream<Level> allLogLevels() {
            return Stream.of(
                    Level.INFO,
                    Level.WARNING,
                    Level.SEVERE,
                    Level.FINE,
                    Level.CONFIG,
                    Level.FINEST,
                    Level.FINER,
                    Level.ALL,
                    Level.OFF);
        }

        static Stream<Arguments> logLevelTestData() {
            return Stream.of(
                    Arguments.of(Level.INFO, "Info message", true),
                    Arguments.of(Level.WARNING, "Warning message", true),
                    Arguments.of(Level.SEVERE, "Error message", true),
                    Arguments.of(Level.FINE, null, false),
                    Arguments.of(Level.CONFIG, null, false)
            );
        }

        @ParameterizedTest
        @MethodSource("logLevelTestData")
        @DisplayName("Should detect log levels correctly")
        void shouldDetectLogLevelsCorrectly(Level level, @SuppressWarnings("unused") String message, boolean shouldExist) {
            // Publish the test levels
            handler.publish(INFO_TEST_RECORD);
            handler.publish(WARNING_TEST_RECORD);
            handler.publish(SEVERE_TEST_RECORD);

            assertEquals(shouldExist, handler.hasLogLevel(level));
        }

        @Test
        @DisplayName("Should return false for non-existent log level")
        void shouldReturnFalseForNonExistentLogLevel() {
            handler.publish(INFO_TEST_RECORD);
            assertFalse(handler.hasLogLevel(Level.SEVERE));
        }

        @ParameterizedTest
        @MethodSource("allLogLevels")
        @DisplayName("Should return false for non-existent log level on empty handler")
        void shouldReturnFalseForNonExistentLogLevelOnEmptyHandler(Level level) {
            assertFalse(handler.hasLogLevel(level));
        }
    }

    @Nested
    @DisplayName("Message Publishing Tests")
    class MessagePublishingTests {

        @ParameterizedTest
        @ValueSource(strings = {"INFO", "WARNING", "SEVERE"})
        @DisplayName("Should publish log record and capture message")
        void shouldPublishLogRecordAndCaptureMessage(String levelName) {
            var level = Level.parse(levelName);
            var message = levelName.toLowerCase() + " message";
            var record = new LogRecord(level, message);

            handler.publish(record);

            var messages = handler.getLogMessages();
            var records = handler.getLogRecords();

            assertEquals(1, messages.size());
            assertEquals(1, records.size());
            assertEquals(message, messages.get(0));
            assertEquals(record.getMessage(), records.get(0).getMessage());
            assertEquals(record.getLevel(), records.get(0).getLevel());

            // Clear for the next iteration
            handler.clear();
        }

        @Test
        @DisplayName("Should publish multiple log records")
        void shouldPublishMultipleLogRecords() {
            var record1 = new LogRecord(Level.INFO, "First message");
            var record2 = new LogRecord(Level.WARNING, "Second message");
            var record3 = new LogRecord(Level.SEVERE, "Third message");

            handler.publish(record1);
            handler.publish(record2);
            handler.publish(record3);

            var messages = handler.getLogMessages();
            var records = handler.getLogRecords();

            assertEquals(3, messages.size());
            assertEquals(3, records.size());
            assertEquals("First message", messages.get(0));
            assertEquals("Second message", messages.get(1));
            assertEquals("Third message", messages.get(2));
        }
    }

    @Nested
    @DisplayName("Message Searching Tests")
    class MessageSearchingTests {

        @BeforeEach
        void setUpMessages() {
            handler.publish(INFO_RECORD_HELLO);
            handler.publish(WARNING_RECORD_WARNING);
            handler.publish(SEVERE_RECORD_ERROR);
            handler.publish(INFO_RECORD_HELLO_AGAIN);
        }

        @ParameterizedTest
        @CsvSource({"Hello, 2", "Warning, 1", "Error, 1", "Debug, 0", "world, 1", "occurred, 1"})
        @DisplayName("Should count messages containing text")
        void shouldCountMessagesContainingText(String searchText, int expectedCount) {
            assertEquals(expectedCount, handler.countMessagesContaining(searchText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Hello world", "Warning: something happened", "Error occurred", "Hello again"})
        @DisplayName("Should find exact messages")
        void shouldFindExactMessages(String exactMessage) {
            assertTrue(handler.containsExactMessage(exactMessage));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Hello", "world", "Warning", "Error", "occurred", "something"})
        @DisplayName("Should find messages containing text")
        void shouldFindMessagesContainingText(String searchText) {
            assertTrue(handler.containsMessage(searchText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Debug", "Non-existent", "xyz", "123"})
        @DisplayName("Should not find non-existent text in messages")
        void shouldNotFindNonExistentTextInMessages(String searchText) {
            assertFalse(handler.containsMessage(searchText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Hello", "world", "Non-existent message", "partial"})
        @DisplayName("Should not find partial matches as exact messages")
        void shouldNotFindPartialMatchesAsExactMessages(String partialMessage) {
            // Only test messages that shouldn't be found as exact matches
            if (!"Hello world".equals(partialMessage) && !"Warning: something happened".equals(partialMessage)
                    && !"Error occurred".equals(partialMessage) && !"Hello again".equals(partialMessage)) {
                assertFalse(handler.containsExactMessage(partialMessage));
            }
        }
    }

    @Nested
    @DisplayName("Null Value Handling Tests")
    class NullValueHandlingTests {

        // Reusable null records
        private static final LogRecord NULL_INFO_RECORD = new LogRecord(Level.INFO, null);
        private static final LogRecord NULL_SEVERE_RECORD = new LogRecord(Level.SEVERE, null);
        private static final LogRecord NULL_WARNING_RECORD = new LogRecord(Level.WARNING, null);

        @ParameterizedTest
        @CsvSource({
                "'', 0",  // Empty string should return 0 for null messages
                "test, 0" // Any non-null string should return 0 for null messages
        })
        @DisplayName("Should handle counting with null messages")
        void shouldHandleCountingWithNullMessages(String searchTerm, int expectedCount) {
            handler.publish(NULL_INFO_RECORD);
            handler.publish(NULL_WARNING_RECORD);

            assertEquals(expectedCount, handler.countMessagesContaining(searchTerm));
        }

        @Test
        @DisplayName("Should handle getFirstRecordContaining with null messages present")
        void shouldHandleGetFirstRecordContainingWithNullMessagesPresent() {
            var validRecord = new LogRecord(Level.WARNING, "Find me");

            handler.publish(NULL_INFO_RECORD);
            handler.publish(validRecord);
            handler.publish(NULL_SEVERE_RECORD);

            var result = handler.getFirstRecordContaining("Find");
            assertTrue(result.isPresent());
            assertEquals("Find me", result.get().getMessage());

            var nullResult = handler.getFirstRecordContaining("nonexistent");
            assertEquals(Optional.empty(), nullResult);
        }

        @Test
        @DisplayName("Should handle LogRecord with null message")
        void shouldHandleLogRecordWithNullMessage() {
            handler.publish(NULL_INFO_RECORD);

            assertEquals(1, handler.getLogRecords().size());
            assertEquals(1, handler.getLogMessages().size());
            assertEquals("", handler.getLogMessages().get(0));
        }

        @Test
        @DisplayName("Should handle mixed null and non-null messages")
        void shouldHandleMixedNullAndNonNullMessages() {
            var validRecord1 = new LogRecord(Level.WARNING, "Valid message");
            var validRecord2 = new LogRecord(Level.INFO, "Another valid message");

            handler.publish(NULL_INFO_RECORD);
            handler.publish(validRecord1);
            handler.publish(NULL_SEVERE_RECORD);
            handler.publish(validRecord2);

            assertEquals(4, handler.getLogRecords().size());
            assertEquals(4, handler.getLogMessages().size());

            // Check null messages are handled correctly
            assertEquals("", handler.getLogMessages().get(0));
            assertEquals("Valid message", handler.getLogMessages().get(1));
            assertEquals("", handler.getLogMessages().get(2));
            assertEquals("Another valid message", handler.getLogMessages().get(3));

            // Search should still work with non-null messages
            assertTrue(handler.containsMessage("Valid"));
            assertTrue(handler.containsMessage("Another"));
            assertEquals(1, handler.countMessagesContaining("Valid"));
            assertEquals(1, handler.countMessagesContaining("Another"));
        }

        @Test
        @DisplayName("Should throw with null log level in hasLogLevel")
        @SuppressWarnings("DataFlowIssue")
        void shouldThrowWithNullLogLevelInHasLogLevel() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            assertThrows(NullPointerException.class, () -> handler.hasLogLevel(null));
        }

        @Test
        @DisplayName("Should throw with null LogRecord")
        @SuppressWarnings("DataFlowIssue")
        void shouldThrowWithNullLogRecord() {
            assertThrows(NullPointerException.class, () -> handler.publish(null));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should throw with null search terms in containsExactMessage")
        void shouldThrowWithNullSearchTermsInContainsExactMessage(String nullSearchTerm) {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            assertThrows(NullPointerException.class, () -> handler.containsExactMessage(nullSearchTerm));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should throw with null search terms in containsMessage")
        void shouldThrowWithNullSearchTermsInContainsMessage(String nullSearchTerm) {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            assertThrows(NullPointerException.class, () -> handler.containsMessage(nullSearchTerm));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should throw with null search terms in countMessagesContaining")
        void shouldThrowWithNullSearchTermsInCountMessagesContaining(String nullSearchTerm) {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            assertThrows(NullPointerException.class, () -> handler.countMessagesContaining(nullSearchTerm));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should throw with null search terms in getFirstRecordContaining")
        void shouldThrowWithNullSearchTermsInGetFirstRecordContaining(String nullSearchTerm) {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            assertThrows(NullPointerException.class, () -> handler.getFirstRecordContaining(nullSearchTerm));
        }

        @Test
        @DisplayName("Should throw on exact message search with null messages present")
        @SuppressWarnings("DataFlowIssue")
        void shouldThrowsOnExactMessageSearchWithNullMessagesPresent() {
            var exactMessageRecord = new LogRecord(Level.WARNING, "Exact message");

            handler.publish(NULL_INFO_RECORD);
            handler.publish(exactMessageRecord);
            handler.publish(NULL_SEVERE_RECORD);

            assertTrue(handler.containsExactMessage("Exact message"));
            assertFalse(handler.containsExactMessage("Exact"));
            assertThrows(NullPointerException.class, () -> handler.containsExactMessage(null));
        }
    }

    @Nested
    @DisplayName("Pattern Matching Tests")
    class PatternMatchingTests {

        private static final LogRecord EMAIL_RECORD = new LogRecord(Level.WARNING, "Invalid email: user@domain.com");
        private static final LogRecord ERROR_RECORD = new LogRecord(Level.INFO, "Error: File not found");
        private static final LogRecord FATAL_RECORD = new LogRecord(Level.SEVERE, "Fatal: System crash");
        private static final LogRecord TEMPERATURE_RECORD = new LogRecord(Level.INFO, "Temperature: 25.5°C");
        // Reusable records for pattern tests
        private static final LogRecord USER_ID_RECORD = new LogRecord(Level.INFO, "User ID: 12345 logged in");
        private static final LogRecord WARNING_RECORD_PATTERN = new LogRecord(Level.WARNING, "Warning: Low disk space");

        @Test
        @DisplayName("Should handle complex regex patterns")
        void shouldHandleComplexRegexPatterns() {
            handler.publish(USER_ID_RECORD);
            handler.publish(EMAIL_RECORD);
            handler.publish(TEMPERATURE_RECORD);

            Pattern userIdPattern = Pattern.compile("User ID: (\\d+)");
            Pattern emailPattern = Pattern.compile("[\\w._%+-]+@[\\w.-]+\\.[A-Z]{2,}");
            Pattern temperaturePattern = Pattern.compile("Temperature: (\\d+\\.\\d+)°C");

            assertTrue(handler.containsMessageMatching(userIdPattern));
            assertTrue(handler.containsMessageMatching(
                    Pattern.compile(emailPattern.pattern(), Pattern.CASE_INSENSITIVE)
            ));
            assertTrue(handler.containsMessageMatching(temperaturePattern));
        }

        @Test
        @DisplayName("Should handle pattern with null messages")
        void shouldHandlePatternWithNullMessages() {
            var validRecord = new LogRecord(Level.WARNING, "Valid message");

            handler.publish(NULL_INFO_RECORD);
            handler.publish(validRecord);

            Pattern pattern = Pattern.compile("Valid.*");
            assertTrue(handler.containsMessageMatching(pattern));
        }

        @Test
        @DisplayName("Should match messages with regex patterns")
        void shouldMatchMessagesWithRegexPatterns() {
            handler.publish(ERROR_RECORD);
            handler.publish(WARNING_RECORD_PATTERN);
            handler.publish(FATAL_RECORD);

            Pattern errorPattern = Pattern.compile("Error:.*");
            Pattern warningPattern = Pattern.compile("Warning:.*");
            Pattern numberPattern = Pattern.compile("\\d+");

            assertTrue(handler.containsMessageMatching(errorPattern));
            assertTrue(handler.containsMessageMatching(warningPattern));
            assertFalse(handler.containsMessageMatching(numberPattern));
        }

        @Test
        @DisplayName("Should throw with null pattern")
        @SuppressWarnings("DataFlowIssue")
        void shouldThrowWithNullPattern() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);
            assertThrows(NullPointerException.class, () -> handler.containsMessageMatching(null));
        }
    }

    @Nested
    @DisplayName("Performance and Stress Tests")
    @ExtendWith(RandomRangeResolver.class)
    class PerformanceAndStressTests {

        // Helper method to populate handler without creating objects in loop
        private void populateHandlerWithTestData(int count) {
            // Create a small pool of reusable records
            var records = new LogRecord[]{
                    new LogRecord(Level.INFO, "Test message 1"),
                    new LogRecord(Level.WARNING, "Test message 2"),
                    new LogRecord(Level.SEVERE, "Test message 3"),
                    new LogRecord(Level.FINE, "Test message 4"),
                    new LogRecord(Level.CONFIG, "Test message 5")
            };

            // Cycle through the pool to populate the handler
            for (int i = 0; i < count; i++) {
                handler.publish(records[i % records.length]);
            }
        }

        @Test
        @DisplayName("Should handle concurrent read operations")
        @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.CloseResource"})
        void shouldHandleConcurrentReadOperations(@RandomRange(min = 7, max = 15) int threadCount)
                throws InterruptedException {
            // Pre-populate with data using batch creation
            populateHandlerWithTestData(1000);

            var executor = Executors.newFixedThreadPool(threadCount);

            // Submit concurrent read operations
            var futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        // Perform various read operations
                        handler.getRecordCount();
                        handler.isEmpty();
                        handler.getLogMessages();
                        handler.getLogRecords();
                        handler.containsMessage("Message");
                        handler.countMessagesContaining("Message");
                        handler.hasLogLevel(Level.INFO);
                        handler.getFirstRecordContaining("Message");
                        handler.getLastRecordContaining("Message");
                        handler.getLastRecord();
                    }, executor))
                    .toArray(CompletableFuture[]::new);

            // Wait for all operations to complete
            CompletableFuture.allOf(futures).join();

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            // Handler state should remain consistent
            assertEquals(1000, handler.getRecordCount());
            assertFalse(handler.isEmpty());
        }

        @Test
        @DisplayName("Should handle frequent clear operations efficiently")
        void shouldHandleFrequentClearOperationsEfficiently() {
            final int iterations = 1000;

            // Reuse records to avoid object creation in the loop
            var messageRecord = new LogRecord(Level.INFO, "Reusable message");
            var warningRecord = new LogRecord(Level.WARNING, "Reusable warning");

            for (int i = 0; i < iterations; i++) {
                // Add some records (reusing existing objects)
                handler.publish(messageRecord);
                handler.publish(warningRecord);

                assertEquals(2, handler.getRecordCount());

                // Clear them
                handler.clear();
                assertEquals(0, handler.getRecordCount());
                assertTrue(handler.isEmpty());
            }
        }

        @Test
        @DisplayName("Should handle large number of records efficiently")
        void shouldHandleLargeNumberOfRecordsEfficiently() {
            final int recordCount = 10000;

            // Measure time for adding records
            long startTime = System.currentTimeMillis();

            populateHandlerWithTestData(recordCount);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            assertEquals(recordCount, handler.getRecordCount());
            assertFalse(handler.isEmpty());

            // Should complete in a reasonable time (less than 5 seconds)
            assertTrue(duration < 5000, "Adding " + recordCount + " records took " + duration + "ms");
        }
    }

    @DisplayName("Print Methods Tests")
    @Nested
    class PrintMethodsTests {

        private TestLogHandler handler;
        private PrintStream originalOut;
        private ByteArrayOutputStream outputStream;
        private PrintStream printStream;

        @Test
        void printEmptyLogMessages() {
            System.setOut(printStream);

            handler.printLogMessages();

            assertEquals("", outputStream.toString());
        }

        @Test
        void printLogMessages() {
            var record1 = new LogRecord(Level.INFO, "First message");
            var record2 = new LogRecord(Level.WARNING, "Second message");
            handler.publish(record1);
            handler.publish(record2);

            System.setOut(printStream);

            handler.printLogMessages();

            var output = outputStream.toString();
            assertTrue(output.contains("First message"));
            assertTrue(output.contains("Second message"));
            assertFalse(output.contains("INFO"));
            assertFalse(output.contains("WARNING"));
        }

        @Test
        void printLogMessagesWithCustomStream() {
            var record1 = new LogRecord(Level.INFO, "Test message 1");
            var record2 = new LogRecord(Level.SEVERE, "Test message 2");
            handler.publish(record1);
            handler.publish(record2);

            handler.printLogMessages(printStream);

            var output = outputStream.toString();
            assertTrue(output.contains("Test message 1"));
            assertTrue(output.contains("Test message 2"));
        }

        @Test
        void printLogMessagesWithLevel() {
            var record1 = new LogRecord(Level.INFO, "Info message");
            var record2 = new LogRecord(Level.WARNING, "Warning message");
            handler.publish(record1);
            handler.publish(record2);

            System.setOut(printStream);

            handler.printLogMessagesWithLevel();

            var output = outputStream.toString();
            assertTrue(output.contains("[INFO] Info message"));
            assertTrue(output.contains("[WARNING] Warning message"));
        }

        @Test
        void printLogMessagesWithLevelAndTimestamp() {
            var record = new LogRecord(Level.WARNING, "Full format message");
            handler.publish(record);

            System.setOut(printStream);

            handler.printLogMessagesWithLevelAndTimestamp();

            var output = outputStream.toString();
            assertTrue(output.contains("Full format message"));
            assertTrue(output.contains("[WARNING]"));
            assertTrue(
                    output.matches("(?s).*\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*] \\[WARNING] Full format message.*")
            );
        }

        @Test
        void printLogMessagesWithLevelAndTimestampCustomStream() {
            var record = new LogRecord(Level.SEVERE, "Complete log entry");
            handler.publish(record);

            handler.printLogMessagesWithLevelAndTimestamp(printStream);

            var output = outputStream.toString();
            assertTrue(output.contains("Complete log entry"));
            assertTrue(output.contains("[SEVERE]"));
            assertTrue(
                    output.matches("(?s).*\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*] \\[SEVERE] Complete log entry.*")
            );
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void printLogMessagesWithLevelAndTimestampNullStream() {
            var record = new LogRecord(Level.INFO, "Test");
            handler.publish(record);

            assertThrows(NullPointerException.class, () -> handler.printLogMessagesWithLevelAndTimestamp(null));
        }

        @Test
        void printLogMessagesWithLevelCustomStream() {
            var record = new LogRecord(Level.SEVERE, "Error occurred");
            handler.publish(record);

            handler.printLogMessagesWithLevel(printStream);

            var output = outputStream.toString();
            assertTrue(output.contains("[SEVERE] Error occurred"));
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void printLogMessagesWithLevelNullStream() {
            var record = new LogRecord(Level.INFO, "Test");
            handler.publish(record);

            assertThrows(NullPointerException.class, () -> handler.printLogMessagesWithLevel(null));
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void printLogMessagesWithNullStream() {
            var record = new LogRecord(Level.INFO, "Test message");
            handler.publish(record);

            assertThrows(NullPointerException.class, () -> handler.printLogMessages(null));
        }

        @Test
        void printLogMessagesWithTimestamp() {
            var record = new LogRecord(Level.INFO, "Timestamped message");
            handler.publish(record);

            System.setOut(printStream);

            handler.printLogMessagesWithTimestamp();

            var output = outputStream.toString();
            assertTrue(output.contains("Timestamped message"));
            assertTrue(output.contains("["));
            assertTrue(output.contains("]"));
            assertTrue(
                    output.matches("(?s).*\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*].*Timestamped message.*")
            );
        }

        @Test
        void printLogMessagesWithTimestampCustomStream() {
            var record = new LogRecord(Level.INFO, "Time test");
            handler.publish(record);

            handler.printLogMessagesWithTimestamp(printStream);

            var output = outputStream.toString();
            assertTrue(output.contains("Time test"));
            assertTrue(output.contains("["));
            assertTrue(output.contains("]"));
            assertTrue(output.matches("(?s).*\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*].*Time test.*"));
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void printLogMessagesWithTimestampNullStream() {
            var record = new LogRecord(Level.INFO, "Test");
            handler.publish(record);

            assertThrows(NullPointerException.class, () -> handler.printLogMessagesWithTimestamp(null));
        }

        @Test
        void printMessagesAfterClear() {
            handler.publish(new LogRecord(Level.INFO, "Before clear"));
            handler.clear();
            handler.publish(new LogRecord(Level.INFO, "After clear"));

            handler.printLogMessages(printStream);

            var output = outputStream.toString();
            assertFalse(output.contains("Before clear"));
            assertTrue(output.contains("After clear"));
        }

        @Test
        void printMessagesPreservesOrder() {
            handler.publish(new LogRecord(Level.INFO, "First"));
            handler.publish(new LogRecord(Level.INFO, "Second"));
            handler.publish(new LogRecord(Level.INFO, "Third"));

            handler.printLogMessages(printStream);

            var output = outputStream.toString();
            int firstIndex = output.indexOf("First");
            int secondIndex = output.indexOf("Second");
            int thirdIndex = output.indexOf("Third");

            assertTrue(firstIndex < secondIndex);
            assertTrue(secondIndex < thirdIndex);
        }

        @Test
        void printMessagesWithDifferentLevels() {
            handler.publish(new LogRecord(Level.FINEST, "Trace"));
            handler.publish(new LogRecord(Level.INFO, "Info"));
            handler.publish(new LogRecord(Level.SEVERE, "Error"));

            handler.printLogMessagesWithLevel(printStream);

            var output = outputStream.toString();
            assertTrue(output.contains("[FINEST] Trace"));
            assertTrue(output.contains("[INFO] Info"));
            assertTrue(output.contains("[SEVERE] Error"));
        }

        @Test
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        void printMultipleMessages() {
            for (int i = 1; i <= 5; i++) {
                var record = new LogRecord(Level.INFO, "Message " + i);
                handler.publish(record);
            }

            System.setOut(printStream);

            handler.printLogMessages();

            var output = outputStream.toString();
            for (int i = 1; i <= 5; i++) {
                assertTrue(output.contains("Message " + i));
            }
        }

        @BeforeEach
        void setUp() {
            handler = new TestLogHandler();
            outputStream = new ByteArrayOutputStream();
            printStream = new PrintStream(outputStream);
            originalOut = System.out;
        }

        @AfterEach
        void tearDown() {
            handler.close();
            printStream.close();
            System.setOut(originalOut);
        }
    }

    @Nested
    @DisplayName("Record Retrieval Tests")
    class RecordRetrievalTests {

        private static final LogRecord DIFFERENT_MESSAGE_RECORD = new LogRecord(Level.SEVERE, "Different message");
        // Reusable records for retrieval tests
        private static final LogRecord FIRST_HELLO_RECORD = new LogRecord(Level.INFO, "First hello message");
        private static final LogRecord SECOND_HELLO_RECORD = new LogRecord(Level.WARNING, "Second hello message");

        static Stream<Arguments> recordRetrievalTestData() {
            return Stream.of(
                    Arguments.of("hello", "First hello message", Level.INFO),
                    Arguments.of("Second", "Second hello message", Level.WARNING),
                    Arguments.of("Different", "Different message", Level.SEVERE)
            );
        }

        @ParameterizedTest
        @MethodSource("recordRetrievalTestData")
        @DisplayName("Should get first record containing text")
        void shouldGetFirstRecordContainingText(String searchText, String expectedMessage, Level expectedLevel) {
            handler.publish(FIRST_HELLO_RECORD);
            handler.publish(SECOND_HELLO_RECORD);
            handler.publish(DIFFERENT_MESSAGE_RECORD);

            var result = handler.getFirstRecordContaining(searchText);

            assertTrue(result.isPresent());
            assertEquals(expectedMessage, result.get().getMessage());
            assertEquals(expectedLevel, result.get().getLevel());
        }

        @ParameterizedTest
        @EmptySource
        @DisplayName("Should handle empty search terms in record retrieval")
        void shouldHandleSearchTermsInRecordRetrieval(String searchTerm) {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            var result = handler.getFirstRecordContaining(searchTerm);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return defensive copies of lists")
        void shouldReturnDefensiveCopiesOfLists() {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            var messages = handler.getLogMessages();
            var records = handler.getLogRecords();

            assertEquals(1, messages.size());
            assertEquals(1, records.size());

            // Modify returned lists - should throw exception for immutable lists
            assertThrows(UnsupportedOperationException.class, () -> messages.add("Modified"));

            var modifiedRecord = new LogRecord(Level.SEVERE, "Modified record");
            assertThrows(UnsupportedOperationException.class, () -> records.add(modifiedRecord));

            // The original handler should be unchanged
            assertEquals(1, handler.getLogMessages().size());
            assertEquals(1, handler.getLogRecords().size());
        }

        @Test
        @DisplayName("Should return null in getLastRecordContaining when record.getMessage() is null")
        @SuppressWarnings("DataFlowIssue")
        void shouldReturnNullInGetLastRecordContainingWhenRecordMessageIsNull() {
            var nullMessageRecord = new LogRecord(Level.INFO, null);
            var anotherNullMessageRecord = new LogRecord(Level.WARNING, null);

            handler.publish(nullMessageRecord);
            handler.publish(anotherNullMessageRecord);

            // No non-null message should always return null for any search
            assertEquals(Optional.empty(), handler.getLastRecordContaining("anything"));
            assertEquals(Optional.empty(), handler.getLastRecordContaining(""));
            assertThrows(NullPointerException.class, () -> handler.getLastRecordContaining(null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"nonexistent", "missing", "not found"})
        @DisplayName("Should return null when no record contains text")
        void shouldReturnNullWhenNoRecordContainsText(String searchText) {
            var someRecord = new LogRecord(Level.INFO, "Some message");
            handler.publish(someRecord);

            var result = handler.getFirstRecordContaining(searchText);

            assertEquals(Optional.empty(), result);
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should throw with null search terms in record retrieval")
        void shouldThrowWithNullAndEmptySearchTermsInRecordRetrieval(String searchTerm) {
            var testRecord = new LogRecord(Level.INFO, "Test message");
            handler.publish(testRecord);

            assertThrows(NullPointerException.class, () -> handler.getFirstRecordContaining(searchTerm));
        }

        @Test
        @DisplayName("Should throw with null messages in getFirstRecordContaining")
        void shouldThrowWithNullMessagesInGetFirstRecordContaining() {
            var validRecord = new LogRecord(Level.WARNING, "Valid message");

            handler.publish(NULL_INFO_RECORD);
            handler.publish(validRecord);

            var result = handler.getFirstRecordContaining("Valid");

            assertTrue(result.isPresent());
            assertEquals(validRecord.getMessage(), result.get().getMessage());
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    @ExtendWith(RandomRangeResolver.class)
    class ThreadSafetyTests {

        // Helper methods to create reusable record pools
        private LogRecord[] createTestRecordPool() {
            return new LogRecord[]{
                    new LogRecord(Level.INFO, "Pool message 1"),
                    new LogRecord(Level.WARNING, "Pool message 2"),
                    new LogRecord(Level.SEVERE, "Pool message 3"),
                    new LogRecord(Level.FINE, "Pool message 4"),
                    new LogRecord(Level.CONFIG, "Pool message 5")
            };
        }

        private LogRecord[][] createThreadRecordPools(int threadCount) {
            var pools = new LogRecord[threadCount][];
            for (int i = 0; i < threadCount; i++) {
                pools[i] = new LogRecord[]{
                        new LogRecord(Level.INFO, "Thread-" + i + "-Info"),
                        new LogRecord(Level.WARNING, "Thread-" + i + "-Warning"),
                        new LogRecord(Level.SEVERE, "Thread-" + i + "-Severe")
                };
            }
            return pools;
        }

        @Test
        @DisplayName("Should handle concurrent clear operations")
        @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.CloseResource"})
        void shouldHandleConcurrentClearOperations() throws InterruptedException {
            // Pre-populate with some data using reusable records
            var records = createTestRecordPool();
            for (int i = 0; i < 100; i++) {
                handler.publish(records[i % records.length]);
            }

            var executor = Executors.newFixedThreadPool(5);

            // Submit concurrent clear and publish operations
            var afterClearRecord = new LogRecord(Level.INFO, "After clear");
            for (int i = 0; i < 5; i++) {
                executor.submit(() -> {
                    handler.clear();
                    handler.publish(afterClearRecord);
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            // Handler should be in a consistent state
            assertNotNull(handler.getLogRecords());
            assertTrue(handler.getRecordCount() <= 5); // At most 5 messages
        }

        @Test
        @DisplayName("Should handle concurrent publishing")
        @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.CloseResource"})
        void shouldHandleConcurrentPublishing(@RandomRange(min = 7, max = 15) int threadCount)
                throws InterruptedException {
            final int messagesPerThread = 100;
            var executor = Executors.newFixedThreadPool(threadCount);

            // Create a pool of reusable records for each thread
            var recordPools = createThreadRecordPools(threadCount);

            // Submit concurrent publishing tasks
            for (int i = 0; i < threadCount; i++) {
                var threadRecords = recordPools[i];
                executor.submit(() -> {
                    for (int j = 0; j < messagesPerThread; j++) {
                        handler.publish(threadRecords[j % threadRecords.length]);
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            assertEquals(threadCount * messagesPerThread, handler.getRecordCount());
            assertFalse(handler.isEmpty());
        }
    }
}