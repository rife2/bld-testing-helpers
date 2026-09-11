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
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.DoNotUseThreads", "unchecked"})
class RetryExtensionTest {

    private Map<Object, Object> backingMap;
    private ExtensionContext mockExtensionContext;
    private Invocation<Void> mockInvocation;
    private ReflectiveInvocationContext<Method> mockInvocationContext;
    private RetryTest mockRetryTest;
    private Method mockTestMethod;
    private RetryExtension retryExtension;

    @BeforeEach
    void beforeEach() {
        mockExtensionContext = mock(ExtensionContext.class);
        ExtensionContext mockParentContext = mock(ExtensionContext.class);
        //noinspection unchecked
        mockInvocation = mock(Invocation.class);
        //noinspection unchecked
        mockInvocationContext = mock(ReflectiveInvocationContext.class);
        mockRetryTest = mock(RetryTest.class);
        mockTestMethod = mock(Method.class);
        retryExtension = new RetryExtension();

        backingMap = new HashMap<>();
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);

        lenient().when(store.get(any())).thenAnswer(inv -> backingMap.get(inv.getArgument(0)));
        lenient().when(store.get(any(), any(Class.class))).thenAnswer(inv -> {
            Object key = inv.getArgument(0);
            Class<?> type = inv.getArgument(1);
            Object val = backingMap.get(key);
            return val == null ? null : type.cast(val);
        });
        lenient().doAnswer(inv -> {
            backingMap.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(store).put(any(), any());
        lenient().when(store.remove(any())).thenAnswer(inv -> backingMap.remove(inv.getArgument(0)));
        lenient().when(store.remove(any(), any(Class.class))).thenAnswer(inv -> {
            Object key = inv.getArgument(0);
            Class<?> type = inv.getArgument(1);
            Object val = backingMap.remove(key);
            return val == null ? null : type.cast(val);
        });
        lenient().when(store.computeIfAbsent(any(), any(Function.class)))
                .thenAnswer(inv -> {
                    Object key = inv.getArgument(0);
                    Function<Object, Object> fn = inv.getArgument(1);
                    return backingMap.computeIfAbsent(key, fn);
                });
        lenient().when(store.computeIfAbsent(any(), any(Function.class), any(Class.class)))
                .thenAnswer(inv -> {
                    Object key = inv.getArgument(0);
                    Function<Object, Object> fn = inv.getArgument(1);
                    return backingMap.computeIfAbsent(key, fn);
                });

        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.of(mockTestMethod));
        when(mockExtensionContext.getRequiredTestMethod()).thenReturn(mockTestMethod);
        when(mockExtensionContext.getParent()).thenReturn(Optional.of(mockParentContext));
        when(mockParentContext.getStore(any())).thenReturn(store);
        when(mockExtensionContext.getStore(any())).thenReturn(store);

        when(mockTestMethod.getAnnotation(RetryTest.class)).thenReturn(mockRetryTest);
        when(mockTestMethod.isAnnotationPresent(RetryTest.class)).thenReturn(true);
        when(mockTestMethod.getName()).thenReturn("testFoo");
        when(mockRetryTest.name()).thenReturn("");
        when(mockRetryTest.value()).thenReturn(3);
        when(mockRetryTest.delay()).thenReturn(0);
        when(mockRetryTest.withExceptions()).thenReturn(new Class[0]);
    }

    private void callAttempt(Invocation<Void> inv) throws Throwable {
        retryExtension.interceptTestTemplateMethod(inv, mockInvocationContext, mockExtensionContext);
    }

    /**
     * Emulates:
     * <pre>
     * static int attempts = 0;
     * &#64;RetryTest(value = 3, delay = 0, withExceptions = IOException.class)
     * void failsOnceThenWouldPass() throws IOException {
     *     if (++attempts < 2) throw new IOException("transient");
     * }
     * </pre>
     * New model: each JUnit invocation context = one call to interceptTestTemplateMethod,
     * state persisted in ExtensionContext.Store.
     */
    @Test
    void failsOnceThenWouldPassEmulatingStaticAttempts() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        when(mockRetryTest.delay()).thenReturn(0);
        when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});

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

        doAnswer(inv -> {
            if (attempts.incrementAndGet() < 2) {
                throw new IOException("transient");
            }
            return null;
        }).when(inv2).proceed();

        doAnswer(inv -> {
            if (attempts.incrementAndGet() < 2) {
                throw new IOException("transient");
            }
            return null;
        }).when(inv3).proceed();

        // attempt 1 – IOException -> retryable, swallowed, delay 0
        assertDoesNotThrow(() -> callAttempt(inv1));
        verify(inv1, times(1)).proceed();
        assertEquals(1, attempts.get());

        // attempt 2 – would pass
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv2, times(1)).proceed();
        assertEquals(2, attempts.get());

        // attempt 3 – outcome already decided, extension short-circuits (finished flag)
        assertDoesNotThrow(() -> callAttempt(inv3));
        verify(inv3, never()).proceed();
        verify(inv3, times(1)).skip();
        assertEquals(2, attempts.get(), "third invocation should not re-run method after success");
    }

    @Test
    void invalidValue() {
        when(mockRetryTest.value()).thenReturn(0);
        assertThrows(ExtensionConfigurationException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));
    }

    @Test
    void noRetryAnnotation() throws Throwable {
        when(mockTestMethod.getAnnotation(RetryTest.class)).thenReturn(null);
        when(mockTestMethod.isAnnotationPresent(RetryTest.class)).thenReturn(false);
        retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext);
        verify(mockInvocation, times(1)).proceed();
    }

    @Test
    void noTestMethod() throws Throwable {
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.empty());
        retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext);
        verify(mockInvocation, times(1)).proceed();
    }

    @Test
    void provideTestTemplateInvocationContexts() {
        when(mockRetryTest.name()).thenReturn("");
        when(mockTestMethod.getName()).thenReturn("testFoo");
        when(mockRetryTest.value()).thenReturn(3);
        var contexts = retryExtension.provideTestTemplateInvocationContexts(mockExtensionContext).toList();
        assertEquals(3, contexts.size());
        assertTrue(contexts.get(0).getDisplayName(1).startsWith("testFoo"));
        assertTrue(contexts.get(0).getDisplayName(1).contains("[1/"));
        when(mockRetryTest.name()).thenReturn("custom name");
        contexts = retryExtension.provideTestTemplateInvocationContexts(mockExtensionContext).toList();
        assertEquals(3, contexts.size());
        assertTrue(contexts.get(0).getDisplayName(1).startsWith("custom name"));
    }

    @Test
    void retryExhausted() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var runtimeException = new RuntimeException("Simulated failure");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doThrow(runtimeException).when(inv1).proceed();
        doThrow(runtimeException).when(inv2).proceed();
        doThrow(runtimeException).when(inv3).proceed();
        assertDoesNotThrow(() -> callAttempt(inv1));
        verify(inv1, times(1)).proceed();
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv2, times(1)).proceed();
        var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv3));
        assertSame(runtimeException, thrown);
        verify(inv3, times(1)).proceed();
    }

    @Test
    void retrySuccess() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        doThrow(new RuntimeException("Simulated failure")).when(inv1).proceed();
        doAnswer(inv -> null).when(inv2).proceed();
        assertDoesNotThrow(() -> callAttempt(inv1));
        verify(inv1, times(1)).proceed();
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv2, times(1)).proceed();
        var inv3 = mock(Invocation.class);
        assertDoesNotThrow(() -> callAttempt(inv3));
        verify(inv3, never()).proceed();
        verify(inv3, times(1)).skip();
    }

    @Test
    void retrySuccessWithCheckedException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        doThrow(new IOException("Simulated failure")).when(inv1).proceed();
        doAnswer(inv -> null).when(inv2).proceed();
        assertDoesNotThrow(() -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv1, times(1)).proceed();
        verify(inv2, times(1)).proceed();
    }

    @Test
    void retryWithCauseChain() throws Throwable {
        when(mockRetryTest.value()).thenReturn(1);
        var root = new IOException("root");
        var wrapper = new RuntimeException(null, root);
        var inv1 = mock(Invocation.class);
        doThrow(wrapper).when(inv1).proceed();
        var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
        assertSame(wrapper, thrown);
    }

    @Test
    void retryWithCheckedException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var checkedException = new IOException("Simulated failure");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doThrow(checkedException).when(inv1).proceed();
        doThrow(checkedException).when(inv2).proceed();
        doThrow(checkedException).when(inv3).proceed();
        assertDoesNotThrow(() -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
        var thrown = assertThrows(IOException.class, () -> callAttempt(inv3));
        assertSame(checkedException, thrown);
    }

    @Test
    void retryWithDelayExhausted() throws Throwable {
        when(mockRetryTest.value()).thenReturn(2);
        when(mockRetryTest.delay()).thenReturn(1);
        var runtimeException = new RuntimeException("Simulated failure");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        doThrow(runtimeException).when(inv1).proceed();
        doThrow(runtimeException).when(inv2).proceed();
        var startTime = System.currentTimeMillis();
        assertDoesNotThrow(() -> callAttempt(inv1));
        var duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 1000, "Expected delay >= 1000ms, was " + duration);
        assertThrows(RuntimeException.class, () -> callAttempt(inv2));
        verify(inv1, times(1)).proceed();
        verify(inv2, times(1)).proceed();
    }

    @Test
    void retryWithDelaySuccess() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        when(mockRetryTest.delay()).thenReturn(1);
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        doThrow(new RuntimeException("Simulated failure")).when(inv1).proceed();
        doAnswer(inv -> null).when(inv2).proceed();
        var startTime = System.currentTimeMillis();
        assertDoesNotThrow(() -> callAttempt(inv1));
        var duration = System.currentTimeMillis() - startTime;
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv1, times(1)).proceed();
        verify(inv2, times(1)).proceed();
        assertTrue(duration >= 1000, "Expected delay >= 1000ms, was " + duration);
    }

    @Test
    void retryWithInterruptedDelay() throws Throwable {
        when(mockRetryTest.value()).thenReturn(2);
        when(mockRetryTest.delay()).thenReturn(5);
        var initialException = new RuntimeException("Initial");
        var inv1 = mock(Invocation.class);
        doThrow(initialException).when(inv1).proceed();
        var testThread = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(500);
                testThread.interrupt();
            } catch (InterruptedException ignored) {
            }
        }).start();
        var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
        assertSame(initialException, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertInstanceOf(InterruptedException.class, thrown.getSuppressed()[0]);
        assertTrue(Thread.currentThread().isInterrupted());
        var ignored = Thread.interrupted();
    }

    @Test
    void retryWithMatchingCauseInChain() throws Throwable {
        when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});
        var root = new IOException("root");
        var wrapper = new RuntimeException(root);
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        doThrow(wrapper).when(inv1).proceed();
        doAnswer(inv -> null).when(inv2).proceed();
        assertDoesNotThrow(() -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv1, times(1)).proceed();
        verify(inv2, times(1)).proceed();
    }

    @Test
    void retryWithMatchingException() throws Throwable {
        when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});
        var ioEx = new IOException("io");
        var inv1 = mock(Invocation.class);
        var inv2 = mock(Invocation.class);
        var inv3 = mock(Invocation.class);
        doThrow(ioEx).when(inv1).proceed();
        doThrow(ioEx).when(inv2).proceed();
        doThrow(ioEx).when(inv3).proceed();
        assertDoesNotThrow(() -> callAttempt(inv1));
        assertDoesNotThrow(() -> callAttempt(inv2));
        var thrown = assertThrows(IOException.class, () -> callAttempt(inv3));
        assertSame(ioEx, thrown);
    }

    @Test
    void retryWithNoMessageException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(1);
        var noMessageEx = new RuntimeException();
        var inv1 = mock(Invocation.class);
        doThrow(noMessageEx).when(inv1).proceed();
        var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
        assertSame(noMessageEx, thrown);
        verify(inv1, times(1)).proceed();
    }

    @Test
    void retryWithNonMatchingExceptionFailsFast() throws Throwable {
        when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});
        var runtimeEx = new RuntimeException("other");
        var inv1 = mock(Invocation.class);
        doThrow(runtimeEx).when(inv1).proceed();
        var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
        assertSame(runtimeEx, thrown);
        verify(inv1, times(1)).proceed();
        var inv2 = mock(Invocation.class);
        assertDoesNotThrow(() -> callAttempt(inv2));
        verify(inv2, never()).proceed();
        verify(inv2, times(1)).skip();
    }

    @Test
    void supportsTestTemplate() {
        assertTrue(retryExtension.supportsTestTemplate(mockExtensionContext));
        when(mockTestMethod.isAnnotationPresent(RetryTest.class)).thenReturn(false);
        assertFalse(retryExtension.supportsTestTemplate(mockExtensionContext));
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.empty());
        assertFalse(retryExtension.supportsTestTemplate(mockExtensionContext));
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
            assertNotNull(stateClass, "RetryState inner class should exist");

            var attemptField = stateClass.getDeclaredField("attempt");
            var finishedField = stateClass.getDeclaredField("finished");

            boolean attemptSafe = java.lang.reflect.Modifier.isVolatile(attemptField.getModifiers())
                    || AtomicInteger.class.isAssignableFrom(attemptField.getType());
            boolean finishedSafe = java.lang.reflect.Modifier.isVolatile(finishedField.getModifiers())
                    || AtomicBoolean.class.isAssignableFrom(finishedField.getType())
                    || AtomicInteger.class.isAssignableFrom(finishedField.getType());

            assertTrue(attemptSafe, "attempt must be volatile or Atomic - plain int causes data race with parallel execution");
            assertTrue(finishedSafe, "finished must be volatile or Atomic - plain boolean causes data race");
        }

        @Test
        @DisplayName("@RetryTest must force SAME_THREAD to prevent concurrent invocations")
        void retryTestMustHaveSameThreadExecutionMode() {
            var exec = RetryTest.class.getAnnotation(Execution.class);
            assertNotNull(exec, "@RetryTest should be meta-annotated with @Execution");
            assertEquals(ExecutionMode.SAME_THREAD, exec.value(),
                    "@RetryTest must use SAME_THREAD - TestTemplate provides all N contexts upfront, "
                            + "so with parallel.enabled=true JUnit would otherwise run BODY 1,2,3 concurrently");
        }
    }

    @Nested
    @DisplayName("Short-circuit must call skip()")
    class ShortCircuitMustCallSkip {

        @Test
        @DisplayName("failing fast on non-retryable exception must skip remaining")
        void failingFastShouldSkipRemaining() throws Throwable {
            when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});
            var inv1 = mock(Invocation.class);
            var inv2 = mock(Invocation.class);

            doThrow(new RuntimeException("non-retryable")).when(inv1).proceed();

            var thrown = assertThrows(RuntimeException.class, () -> callAttempt(inv1));
            assertEquals("non-retryable", thrown.getMessage());

            // second invocation should be skipped because finished=true after fail-fast
            assertDoesNotThrow(() -> callAttempt(inv2));
            verify(inv2, never()).proceed();
            verify(inv2, times(1)).skip();
        }

        @Test
        @DisplayName("passing on first attempt should skip remaining, not violate interceptor contract")
        void passingFirstAttemptShouldSkipRemaining() throws Throwable {
            when(mockRetryTest.value()).thenReturn(3);
            var inv1 = mock(Invocation.class);
            var inv2 = mock(Invocation.class);
            var inv3 = mock(Invocation.class);

            doAnswer(inv -> null).when(inv1).proceed();

            // attempt 1 passes -> finished=true
            assertDoesNotThrow(() -> callAttempt(inv1));
            verify(inv1, times(1)).proceed();
            verify(inv1, never()).skip();

            // attempts 2 and 3 must call skip(), not proceed(), and must not throw JUnitException
            assertDoesNotThrow(() -> callAttempt(inv2));
            assertDoesNotThrow(() -> callAttempt(inv3));

            verify(inv2, never()).proceed();
            verify(inv2, times(1)).skip();

            verify(inv3, never()).proceed();
            verify(inv3, times(1)).skip();
        }
    }
}