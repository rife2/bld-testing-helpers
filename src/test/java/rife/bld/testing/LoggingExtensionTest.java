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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "PMD.AvoidDuplicateLiterals"})
class LoggingExtensionTest {

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private static Object getPrivateField(Object obj, String field) {
        try {
            var f = LoggingExtension.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get field: " + field, e);
        }
    }

    private static Logger getRandomLogger() {
        return Logger.getLogger(TestingUtils.generateRandomString());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> getTestConfigsForTestClass(Class<?> testClass) throws ReflectiveOperationException {
        var field = LoggingExtension.class.getDeclaredField("testMethodConfigs");
        field.setAccessible(true);
        var configs = (Map<Class<?>, Map<String, ?>>) field.get(null);
        return configs.getOrDefault(testClass, new ConcurrentHashMap<>());
    }

    /**
     * Creates a mock ExtensionContext that returns {@code testClass} from getRequiredTestClass()
     * and a stable unique ID derived from the class name so that beforeEach and afterEach
     * lookups always use the same key.
     */
    private static ExtensionContext mockExtensionContext(Class<?> testClass) {
        var context = mock(ExtensionContext.class);
        // ExtensionContext.getRequiredTestClass() is a final method, so use doReturn...when... syntax
        doReturn(testClass).when(context).getRequiredTestClass();
        // Stub getUniqueId() with a stable value so beforeEach and afterEach use the same map key
        doReturn("[test:" + testClass.getName() + "]").when(context).getUniqueId();
        return context;
    }

    @BeforeEach
    void beforeEach() throws ReflectiveOperationException {
        var field = LoggingExtension.class.getDeclaredField("testMethodConfigs");
        field.setAccessible(true);
        var configs = (Map<?, ?>) field.get(null);
        configs.clear();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        void defaultConstructorUsesDefaultLoggerAndAllLevel() {
            var extension = new LoggingExtension();
            assertNotNull(getPrivateField(extension, "logger"));
            assertEquals(Level.ALL, getPrivateField(extension, "level"));
        }

        @Test
        void loggerAndHandlerConstructorSetsLoggerAndHandler() {
            var logger = getRandomLogger();
            var handler = new ConsoleHandler();
            var extension = new LoggingExtension(logger, handler);
            assertSame(logger, getPrivateField(extension, "logger"));
            assertSame(handler, getPrivateField(extension, "handler"));
            assertEquals(handler.getLevel(), getPrivateField(extension, "level"));
        }

        @Test
        void loggerAndLevelConstructorSetsLoggerAndLevel() {
            var logger = getRandomLogger();
            var extension = new LoggingExtension(logger, Level.WARNING);
            assertSame(logger, getPrivateField(extension, "logger"));
            assertEquals(Level.WARNING, getPrivateField(extension, "level"));
        }

        @Test
        void loggerAndNullHandlerConstructorUsesAllLevel() {
            var logger = getRandomLogger();
            var extension = new LoggingExtension(logger, (Handler) null);
            assertEquals(Level.ALL, getPrivateField(extension, "level"));
        }

        @Test
        void loggerHandlerLevelConstructorSetsAllFields() {
            var logger = getRandomLogger();
            var handler = new ConsoleHandler();
            var extension = new LoggingExtension(logger, handler, Level.SEVERE);
            assertSame(logger, getPrivateField(extension, "logger"));
            assertSame(handler, getPrivateField(extension, "handler"));
            assertEquals(Level.SEVERE, getPrivateField(extension, "level"));
        }

        @Test
        void loggerOnlyConstructorSetsLoggerAndAllLevel() {
            var logger = getRandomLogger();
            var extension = new LoggingExtension(logger);
            assertSame(logger, getPrivateField(extension, "logger"));
            assertEquals(Level.ALL, getPrivateField(extension, "level"));
        }

        @Test
        void stringLoggerAndHandlerConstructorCreatesLoggerWithHandler() {
            var testLoggerName = TestingUtils.generateRandomString();
            var handler = new ConsoleHandler();
            var extension = new LoggingExtension(testLoggerName, handler);
            var logger = (Logger) getPrivateField(extension, "logger");
            assertEquals(testLoggerName, logger.getName());
            assertSame(handler, getPrivateField(extension, "handler"));
        }

        @Test
        void stringLoggerAndLevelConstructorCreatesLoggerWithLevel() {
            var testLoggerName = TestingUtils.generateRandomString();
            var extension = new LoggingExtension(testLoggerName, Level.FINE);
            var logger = (Logger) getPrivateField(extension, "logger");
            assertEquals(testLoggerName, logger.getName());
            assertEquals(Level.FINE, getPrivateField(extension, "level"));
        }

        @Test
        void stringLoggerConstructorCreatesLogger() {
            var testLoggerName = TestingUtils.generateRandomString();
            var extension = new LoggingExtension(testLoggerName);
            var logger = (Logger) getPrivateField(extension, "logger");
            assertEquals(testLoggerName, logger.getName());
        }

        @Test
        void stringLoggerHandlerLevelConstructorCreatesLoggerWithHandlerAndLevel() {
            var testLoggerName = TestingUtils.generateRandomString();
            var handler = new ConsoleHandler();
            var extension = new LoggingExtension(testLoggerName, handler, Level.OFF);
            var logger = (Logger) getPrivateField(extension, "logger");
            assertEquals(testLoggerName, logger.getName());
            assertSame(handler, getPrivateField(extension, "handler"));
            assertEquals(Level.OFF, getPrivateField(extension, "level"));
        }
    }

    @Nested
    @DisplayName("Coverage Edge Cases")
    class CoverageEdgeCases {

        @Test
        void afterEachWithNullAddedHandlerAndNullOriginalHandlerLevel() throws ReflectiveOperationException {
            var logger = getRandomLogger();
            var extension = new LoggingExtension(logger);
            var context = mockExtensionContext(this.getClass());

            // Create LoggerState instance with null handler via reflection
            var loggerStateClass = Class.forName("rife.bld.testing.LoggingExtension$LoggerState");
            var ctor = loggerStateClass.getDeclaredConstructor(Logger.class, Handler.class);
            ctor.setAccessible(true);
            Object loggerState = ctor.newInstance(logger, null);

            // Insert into TEST_METHOD_CONFIGS using the same key that afterEach will look up
            var field = LoggingExtension.class.getDeclaredField("testMethodConfigs");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var configs = (Map<Class<?>, Map<String, Object>>) field.get(null);

            Map<String, Object> map = new ConcurrentHashMap<>();
            map.put(context.getUniqueId(), loggerState);  // key must match getUniqueId()
            configs.put(this.getClass(), map);

            // Should not throw, covers (addedHandler == null) and (originalHandlerLevel == null)
            assertDoesNotThrow(() -> extension.afterEach(context));
        }
    }

    @Nested
    @DisplayName("After Each Tests")
    class afterEachTests {

        private static Object invokeAccessor(Object record, String name) throws ReflectiveOperationException {
            var method = record.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return method.invoke(record);
        }

        @Test
        void afterEachRestoresLoggerAndHandlerState() throws ReflectiveOperationException {
            var logger = getRandomLogger();
            var handler = new ConsoleHandler();
            logger.addHandler(handler);
            logger.setUseParentHandlers(true);
            logger.setLevel(Level.SEVERE);

            var extension = new LoggingExtension(logger, Level.INFO);

            var context = mockExtensionContext(this.getClass());

            extension.beforeEach(context);

            assertEquals(Level.INFO, logger.getLevel());
            assertFalse(logger.getUseParentHandlers());

            extension.afterEach(context);

            assertTrue(Arrays.asList(logger.getHandlers()).contains(handler));
            assertEquals(Level.SEVERE, logger.getLevel());
            assertTrue(logger.getUseParentHandlers());

            var map = getTestConfigsForTestClass(this.getClass());
            assertTrue(map.isEmpty(), "Configurations should be cleared after afterEach");
        }

        @Test
        void afterEachWhenNoConfigsDoesNothing() {
            var logger = getRandomLogger();
            var extension = new LoggingExtension(logger);
            var context = mockExtensionContext(this.getClass());
            // Not calling beforeEach(), so config map is empty
            assertDoesNotThrow(() -> extension.afterEach(context));
        }

        @Test
        void afterEachWithNonNullHandlerDoesNotCloseConsoleHandler() {
            var logger = getRandomLogger();
            var handler = new ConsoleHandler();
            var extension = new LoggingExtension(logger, handler); // handler != null
            var context = mockExtensionContext(this.getClass());

            extension.beforeEach(context);

            // Should skip close() on handler; branch handler != null
            assertDoesNotThrow(() -> extension.afterEach(context));
        }

        @Test
        void afterEachWithNullLoggerNameRestoresThisLogger() {
            // Custom Logger with null name
            var nullNameLogger = new Logger(null, null) {
            };
            var extension = new LoggingExtension(nullNameLogger);
            var context = mockExtensionContext(this.getClass());

            // beforeEach stores state under context.getUniqueId(); afterEach retrieves it the same way,
            // so a null logger name is no longer a special case
            extension.beforeEach(context);
            assertDoesNotThrow(() -> extension.afterEach(context));
        }

        @Test
        void afterEachWithNullOriginalHandlerLevelDoesNotSetLevel() throws ReflectiveOperationException {
            var logger = getRandomLogger();
            var extension = new LoggingExtension(logger);
            var context = mockExtensionContext(this.getClass());
            extension.beforeEach(context);

            // Replace the stored state with a copy that has originalHandlerLevel = null
            var configs = getTestConfigsForTestClass(this.getClass());
            var configKey = context.getUniqueId();
            var existingState = configs.get(configKey);

            // Get the canonical record constructor via reflection
            var stateClass = existingState.getClass();
            var ctor = stateClass.getDeclaredConstructors()[1];
            ctor.setAccessible(true);

            // Read current field values from the existing state
            var targetLogger = invokeAccessor(existingState, "targetLogger");
            var originalLevel = invokeAccessor(existingState, "originalLevel");
            var useParent = invokeAccessor(existingState, "originalUseParentHandlers");
            var origHandlers = invokeAccessor(existingState, "originalHandlers");
            var addedHandler = invokeAccessor(existingState, "addedHandler");

            // Construct a new state with originalHandlerLevel = null
            var patchedState = ctor.newInstance(
                    targetLogger,
                    originalLevel,
                    (boolean) useParent,
                    origHandlers,
                    addedHandler,
                    null);
            //noinspection unchecked
            ((Map<String, Object>) configs).put(configKey, patchedState);

            // Should take the else branch (do nothing) without throwing
            assertDoesNotThrow(() -> extension.afterEach(context));
        }

        @Test
        void afterEachWithTestLogHandlerClearsRecords() {
            var logger = getRandomLogger();
            var testHandler = new TestLogHandler();
            var extension = new LoggingExtension(logger, testHandler, Level.FINE);

            var context = mockExtensionContext(this.getClass());

            extension.beforeEach(context);

            var record = new LogRecord(Level.FINE, "test message");
            testHandler.publish(record);
            assertFalse(testHandler.isEmpty());

            extension.afterEach(context);

            assertTrue(testHandler.isEmpty(), "TestLogHandler records should be cleared");
        }
    }

    @Nested
    @DisplayName("Before Each Tests")
    class beforeEachTests {

        @Test
        void beforeEachAddsHandlerAndConfiguresLogger() throws ReflectiveOperationException {
            var logger = getRandomLogger();
            var extension = new LoggingExtension(logger, Level.FINE);

            var context = mockExtensionContext(this.getClass());

            assertEquals(0, logger.getHandlers().length);

            extension.beforeEach(context);

            assertEquals(1, logger.getHandlers().length);
            assertEquals(Level.FINE, logger.getHandlers()[0].getLevel());
            assertEquals(Level.FINE, logger.getLevel());
            assertFalse(logger.getUseParentHandlers());

            var map = getTestConfigsForTestClass(this.getClass());
            assertFalse(map.isEmpty());
        }

        @Test
        void beforeEachWithCustomHandlerPreservesOriginalHandlerLevel() {
            var logger = getRandomLogger();
            var handler = new ConsoleHandler();
            handler.setLevel(Level.WARNING);
            var extension = new LoggingExtension(logger, handler);

            var context = mockExtensionContext(this.getClass());

            extension.beforeEach(context);
            assertEquals(Level.WARNING, handler.getLevel());
        }
    }
}