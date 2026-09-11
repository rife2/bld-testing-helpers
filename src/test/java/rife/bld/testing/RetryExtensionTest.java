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
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.DoNotUseThreads", "unchecked",
        "PMD.SignatureDeclareThrowsException", "PMD.TestClassWithoutTestCases"})
class RetryExtensionTest {

    private Map<Object, Object> backingMap;
    private ExtensionContext mockExtensionContext;
    private ReflectiveInvocationContext<Method> mockInvocationContext;
    private RetryExtension retryExtension;

    @BeforeEach
    void beforeEach() throws Exception {
        backingMap = new HashMap<>();
        mockExtensionContext = mock(ExtensionContext.class);
        ExtensionContext mockParentContext = mock(ExtensionContext.class);
        mockInvocationContext = mock(ReflectiveInvocationContext.class);
        retryExtension = new RetryExtension();
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);

        lenient().when(store.get(any())).thenAnswer(inv -> backingMap.get(inv.getArgument(0)));
        lenient().when(store.get(any(), any(Class.class))).thenAnswer(inv -> {
            Object key = inv.getArgument(0);
            Class<?> type = inv.getArgument(1);
            Object val = backingMap.get(key);
            return val == null ? null : type.cast(val);
        });
        lenient().doAnswer(inv -> backingMap.put(inv.getArgument(0), inv.getArgument(1))).when(store).put(any(), any());
        lenient().when(store.remove(any())).thenAnswer(inv -> backingMap.remove(inv.getArgument(0)));
        lenient().when(store.remove(any(), any(Class.class))).thenAnswer(inv -> {
            Object key = inv.getArgument(0);
            Class<?> type = inv.getArgument(1);
            Object val = backingMap.remove(key);
            return val == null ? null : type.cast(val);
        });
        lenient().when(store.computeIfAbsent(any(), any(Function.class))).thenAnswer(inv -> {
            Object key = inv.getArgument(0);
            Function<Object, Object> fn = inv.getArgument(1);
            return backingMap.computeIfAbsent(key, fn);
        });
        lenient().when(store.computeIfAbsent(any(), any(Function.class), any(Class.class))).thenAnswer(inv -> {
            Object key = inv.getArgument(0);
            Function<Object, Object> fn = inv.getArgument(1);
            return backingMap.computeIfAbsent(key, fn);
        });

        var currentMethod = TestScenarios.class.getDeclaredMethod("default3");
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.of(currentMethod));
        when(mockExtensionContext.getRequiredTestMethod()).thenReturn(currentMethod);
        when(mockExtensionContext.getParent()).thenReturn(Optional.of(mockParentContext));
        when(mockParentContext.getStore(any())).thenReturn(store);
        when(mockExtensionContext.getStore(any())).thenReturn(store);
    }

    private void callAttempt(Invocation<Void> inv) throws Throwable {
        retryExtension.interceptTestTemplateMethod(inv, mockInvocationContext, mockExtensionContext);
    }

    @Test
    void failsOnceThenWouldPassEmulatingStaticAttempts() throws Throwable {
        setMethod("ioOnly");
        var attempts = new AtomicInteger(0);
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doAnswer(inv -> {
            if (attempts.incrementAndGet() < 2) {
                throw new IOException("transient");
            }
            return null;
        }).when(inv1).proceed();
        doAnswer(inv -> null).when(inv2).proceed();
        doAnswer(inv -> null).when(inv3).proceed();

        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
        assertDoesNotThrow(() -> callAttempt(inv3));
        verify(inv3, times(1)).skip();
    }

    @Test
    void invalidValue() throws Exception {
        setMethod("zero");
        var inv = mock(Invocation.class);
        assertThrows(ExtensionConfigurationException.class, () -> callAttempt(inv));
    }

    @Test
    void noRetryAnnotation() throws Throwable {
        setMethod("noAnnotation");
        var inv = mock(Invocation.class);
        doAnswer(a -> null).when(inv).proceed();
        assertDoesNotThrow(() -> callAttempt(inv));
        verify(inv, times(1)).proceed();
    }

    @Test
    void provideContextsNoAnnotation() throws Exception {
        setMethod("noAnnotation");
        assertThrows(ExtensionConfigurationException.class,
                () -> retryExtension.provideTestTemplateInvocationContexts(mockExtensionContext).toList());
    }

    @Test
    void provideTestTemplateInvocationContexts() throws Exception {
        setMethod("default3");
        var contexts = retryExtension.provideTestTemplateInvocationContexts(mockExtensionContext).toList();
        assertEquals(3, contexts.size());
    }

    @Test
    void retryExhausted() throws Throwable {
        setMethod("default3");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doThrow(new RuntimeException("fail")).when(inv1).proceed();
        doThrow(new RuntimeException("fail")).when(inv2).proceed();
        doThrow(new RuntimeException("fail")).when(inv3).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertThrows(TestAbortedException.class, () -> callAttempt(inv2));
        assertThrows(RuntimeException.class, () -> callAttempt(inv3));
    }

    @Test
    void retrySuccess() throws Throwable {
        setMethod("default3");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doThrow(new RuntimeException("fail")).when(inv1).proceed();
        doAnswer(a -> null).when(inv2).proceed();
        doAnswer(a -> null).when(inv3).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
        assertDoesNotThrow(() -> callAttempt(inv3));
        verify(inv3).skip();
    }

    @Test
    void retrySuccessWithCheckedException() throws Throwable {
        setMethod("ioOnly");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        doThrow(new IOException("io")).when(inv1).proceed();
        doAnswer(a -> null).when(inv2).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
    }

    @Test
    void retryWithCauseChain() throws Throwable {
        setMethod("ioOnly");
        var inv1 = mock(Invocation.class);
        var cause = new IOException("cause");
        var wrapper = new RuntimeException(cause);
        doThrow(wrapper).when(inv1).proceed();
        var inv2 = mock(Invocation.class);
        doAnswer(a -> null).when(inv2).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv2, times(1)).proceed();
    }

    @Test
    void retryWithCheckedException() throws Throwable {
        setMethod("ioOnly");
        var inv1 = mock(Invocation.class);
        doThrow(new IOException("io")).when(inv1).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
    }

    @Test
    void retryWithDelayExhausted() throws Throwable {
        setMethod("delay1");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doThrow(new RuntimeException("fail")).when(inv1).proceed();
        doThrow(new RuntimeException("fail")).when(inv2).proceed();
        doThrow(new RuntimeException("fail")).when(inv3).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertThrows(TestAbortedException.class, () -> callAttempt(inv2));
        assertThrows(RuntimeException.class, () -> callAttempt(inv3));
    }

    @Test
    void retryWithDelaySuccess() throws Throwable {
        setMethod("delay1");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        doThrow(new RuntimeException("fail")).when(inv1).proceed();
        doAnswer(a -> null).when(inv2).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
    }

    @Test
    void retryWithInterruptedDelay() throws Throwable {
        setMethod("delay1");
        var inv1 = mock(Invocation.class);
        doThrow(new RuntimeException("Initial")).when(inv1).proceed();
        Thread.currentThread().interrupt();
        try {
            var thrown = assertThrows(InterruptedException.class, () -> callAttempt(inv1));
            // first failure triggers sleep -> interrupted, so original is rethrown, not aborted
            assertEquals("sleep interrupted", thrown.getMessage());
            // InterruptedException must not have a suppressed exception (unlike old implementation)
            assertEquals(0, thrown.getSuppressed().length,
                    "InterruptedException should not carry suppressed exceptions");
        } finally {
            var ignored = Thread.interrupted();
        }
        // Second invocation should be skipped because state.finished was set
        var inv2 = mock(Invocation.class);
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv2, times(1)).skip();
    }

    @Test
    void retryWithMatchingCauseInChain() throws Throwable {
        setMethod("ioOnly");
        var inv1 = mock(Invocation.class);
        var io = new IOException("io");
        var wrapped = new RuntimeException(io);
        doThrow(wrapped).when(inv1).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
    }

    @Test
    void retryWithMatchingException() throws Throwable {
        setMethod("ioOnly");
        var ioEx = new IOException("io");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doThrow(ioEx).when(inv1).proceed();
        doThrow(ioEx).when(inv2).proceed();
        doThrow(ioEx).when(inv3).proceed();
        assertThrows(TestAbortedException.class, () -> callAttempt(inv1));
        assertThrows(TestAbortedException.class, () -> callAttempt(inv2));
        var thrown = assertThrows(IOException.class, () -> callAttempt(inv3));
        assertSame(ioEx, thrown);
    }

    @Test
    void retryWithNoMessageException() throws Throwable {
        setMethod("one");
        var inv1 = mock(Invocation.class);
        var noMessageEx = new RuntimeException();
        doThrow(noMessageEx).when(inv1).proceed();
        var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
        assertSame(noMessageEx, thrown);
    }

    @Test
    void retryWithNonMatchingExceptionFailsFast() throws Throwable {
        setMethod("ioOnly");
        var runtimeEx = new RuntimeException("other");
        var inv1 = mock(Invocation.class);
        doThrow(runtimeEx).when(inv1).proceed();
        var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
        assertSame(runtimeEx, thrown);
        var inv2 = mock(Invocation.class);
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv2, never()).proceed();
        verify(inv2, times(1)).skip();
    }

    private void setMethod(String name) throws Exception {
        var currentMethod = TestScenarios.class.getDeclaredMethod(name);
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.of(currentMethod));
        when(mockExtensionContext.getRequiredTestMethod()).thenReturn(currentMethod);
        backingMap.clear();
    }

    @Test
    void supportsTestTemplate() throws Exception {
        setMethod("default3");
        assertTrue(retryExtension.supportsTestTemplate(mockExtensionContext));
        setMethod("noAnnotation");
        assertFalse(retryExtension.supportsTestTemplate(mockExtensionContext));
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.empty());
        assertFalse(retryExtension.supportsTestTemplate(mockExtensionContext));
    }

    @SuppressWarnings({"DefaultAnnotationParam", "PMD.TestClassWithoutTestCases", "EmptyMethod"})
    @Disabled("Deliberately invalid — reflection fixture only, never meant to run")
    static class TestScenarios {

        @RetryTest(3)
        void default3() {
            // keep empty
        }

        @RetryTest(value = 3, delay = 1)
        void delay1() {
            // keep empty
        }

        @RetryTest(value = 3, delay = 0, withExceptions = IOException.class)
        void ioOnly() {
            // keep empty
        }

        @SuppressWarnings("unused")
        void noAnnotation() {
            // keep empty
        }

        @RetryTest(1)
        void one() {
            // keep empty
        }

        @RetryTest(0)
        void zero() {
            // keep empty
        }
    }

    @Nested
    @DisplayName("Cycle-safe getMessageRecursively")
    class CycleSafeMessage {

        @Test
        @DisplayName("getMessageRecursively does not StackOverflow on cyclic cause chain")
        void doesNotOverflowOnCycle() {
            var ex1 = new RuntimeException("ex1");
            var ex2 = new RuntimeException("ex2"); // ex2 -> ex1
            ex1.initCause(ex2); // ex1 -> ex2 -> ex1 = cycle, only one initCause call

            assertDoesNotThrow(() -> invokeGetMessageRecursively(ex1),
                    "cycle should not cause StackOverflowError");
        }

        @Test
        @DisplayName("getMessageRecursively handles exception with no message")
        void handlesNoMessage() throws Exception {
            var ex = new RuntimeException();
            var msg = invokeGetMessageRecursively(ex);
            assertTrue(msg.startsWith("No message ["), "got: " + msg);
        }

        @Test
        @DisplayName("getMessageRecursively handles null throwable")
        void handlesNull() throws Exception {
            var msg = invokeGetMessageRecursively(null);
            assertEquals("Unknown error", msg);
        }

        @SuppressWarnings({"PMD.AvoidAccessibilityAlteration"})
        private String invokeGetMessageRecursively(Throwable t) throws Exception {
            var m = RetryExtension.class.getDeclaredMethod("getMessageRecursively", Throwable.class);
            m.setAccessible(true);
            return (String) m.invoke(retryExtension, t);
        }

        @Test
        @DisplayName("matchesCauseChain does not StackOverflow on cyclic chain")
        void matchesCauseChainDoesNotOverflowOnCycle() throws Throwable {
            setMethod("ioOnly"); // withExceptions = IOException
            var ex1 = new RuntimeException("ex1");
            var ex2 = new IOException("io in cycle"); // ex2 -> ex1
            ex1.initCause(ex2); // ex1 -> ex2 -> ex1 = cycle, only one initCause call

            var inv = mock(Invocation.class);
            doThrow(ex1).when(inv).proceed();

            // should be retryable (contains IOException in cycle) and not overflow
            assertThrows(TestAbortedException.class, () -> callAttempt(inv));
        }

        @Test
        @DisplayName("Retry with cyclic exception chain does not overflow in printError path")
        void retryWithCyclicDoesNotOverflowEndToEnd() throws Throwable {
            setMethod("one"); // no filter, any exception retryable
            var ex1 = new RuntimeException();
            var ex2 = new RuntimeException(ex1); // ex2 -> ex1
            ex1.initCause(ex2); // ex1 -> ex2 -> ex1 = cycle, only one initCause call

            var inv = mock(Invocation.class);
            doThrow(ex1).when(inv).proceed();

            // intercept catches, calls printError -> getMessageRecursively
            // must not throw StackOverflowError, should throw original on last attempt
            var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv));
            assertSame(ex1, thrown);
        }

        @Test
        @DisplayName("getMessageRecursively returns localized message with class name")
        void returnsMessageWithClassName() throws Exception {
            var ex = new IOException("disk full");
            var msg = invokeGetMessageRecursively(ex);
            assertTrue(msg.contains("disk full"));
            assertTrue(msg.contains(IOException.class.getName()));
        }

        @Test
        @DisplayName("getMessageRecursively walks cause chain to find message")
        void walksCauseChain() throws Exception {
            var root = new IOException("root cause");
            var mid = new RuntimeException(null, root);
            var top = new RuntimeException(null, mid);
            var msg = invokeGetMessageRecursively(top);
            assertTrue(msg.contains("root cause"), "should find root message, got: " + msg);
        }
    }

    @Nested
    @DisplayName("Parallel execution and data race")
    class ParallelExecutionRegression {

        @Test
        @DisplayName("RetryState fields must be volatile or atomic to avoid data race")
        void retryStateFieldsMustBeSafeForConcurrentVisibility() throws Exception {
            Class<?> stateClass = null;
            for (var inner : RetryExtension.class.getDeclaredClasses()) {
                if ("RetryState".equals(inner.getSimpleName())) {
                    stateClass = inner;
                    break;
                }
            }
            assertNotNull(stateClass);
            var attemptField = stateClass.getDeclaredField("attempt");
            var finishedField = stateClass.getDeclaredField("finished");
            boolean attemptSafe = java.lang.reflect.Modifier.isVolatile(attemptField.getModifiers())
                    || AtomicInteger.class.isAssignableFrom(attemptField.getType());
            boolean finishedSafe = java.lang.reflect.Modifier.isVolatile(finishedField.getModifiers())
                    || AtomicBoolean.class.isAssignableFrom(finishedField.getType());
            assertTrue(attemptSafe);
            assertTrue(finishedSafe);
        }

        @Test
        @DisplayName("@RetryTest must force SAME_THREAD to prevent concurrent invocations")
        void retryTestMustHaveSameThreadExecutionMode() {
            var exec = RetryTest.class.getAnnotation(Execution.class);
            assertNotNull(exec);
            assertEquals(ExecutionMode.SAME_THREAD, exec.value());
        }
    }

    @Nested
    @DisplayName("publishReportEntry is used for error reporting")
    class PublishReportEntryOnFailure {

        @Test
        @DisplayName("publishReportEntry is NOT called when a test succeeds")
        void doesNotPublishReportEntryOnSuccess() throws Throwable {
            setMethod("default3");
            var inv = mock(Invocation.class);
            doAnswer(a -> null).when(inv).proceed();
            assertDoesNotThrow(() -> callAttempt(inv));
            verify(mockExtensionContext, never()).publishReportEntry(anyString(), anyString());
        }

        @Test
        @DisplayName("publishReportEntry is NOT called when no annotation is present")
        void doesNotPublishReportEntryWithoutAnnotation() throws Throwable {
            setMethod("noAnnotation");
            var inv = mock(Invocation.class);
            doAnswer(a -> null).when(inv).proceed();
            assertDoesNotThrow(() -> callAttempt(inv));
            verify(mockExtensionContext, never()).publishReportEntry(anyString(), anyString());
        }

        @Test
        @DisplayName("publishReportEntry is called when a non-retryable failure occurs")
        void publishesReportEntryOnNonRetryableFailure() throws Throwable {
            setMethod("ioOnly");
            var inv = mock(Invocation.class);
            doThrow(new RuntimeException("not-retryable")).when(inv).proceed();
            assertThrows(RuntimeException.class, () -> callAttempt(inv));
            verify(mockExtensionContext).publishReportEntry(
                    contains("Retry #1 failed"), contains("not-retryable"));
        }

        @Test
        @DisplayName("publishReportEntry is called when a retryable failure occurs")
        void publishesReportEntryOnRetryableFailure() throws Throwable {
            setMethod("default3");
            var inv = mock(Invocation.class);
            doThrow(new RuntimeException("boom")).when(inv).proceed();
            assertThrows(TestAbortedException.class, () -> callAttempt(inv));
            verify(mockExtensionContext).publishReportEntry(contains("Retry #1 failed"), contains("boom"));
        }
    }

    @Nested
    @DisplayName("Short-circuit must call skip()")
    class ShortCircuitMustCallSkip {

        @Test
        @DisplayName("failing fast on non-retryable exception must skip remaining")
        void failingFastShouldSkipRemaining() throws Throwable {
            setMethod("ioOnly");
            var inv1 = mock(Invocation.class);
            var inv2 = mock(Invocation.class);
            doThrow(new RuntimeException("non-retryable")).when(inv1).proceed();
            var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
            assertEquals("non-retryable", thrown.getMessage());
            assertDoesNotThrow(() -> callAttempt(inv2));
            verify(inv2, never()).proceed();
            verify(inv2, times(1)).skip();
        }

        @Test
        @DisplayName("passing on first attempt should skip remaining, not violate interceptor contract")
        void passingFirstAttemptShouldSkipRemaining() throws Throwable {
            setMethod("default3");
            var inv1 = mock(Invocation.class);
            var inv2 = mock(Invocation.class);
            var inv3 = mock(Invocation.class);
            doAnswer(inv -> null).when(inv1).proceed();
            assertDoesNotThrow(() -> callAttempt(inv1));
            assertDoesNotThrow(() -> callAttempt(inv2));
            assertDoesNotThrow(() -> callAttempt(inv3));
            verify(inv2, never()).proceed();
            verify(inv2, times(1)).skip();
            verify(inv3, never()).proceed();
            verify(inv3, times(1)).skip();
        }
    }
}
