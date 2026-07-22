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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.DoNotUseThreads"})
class RetryExtensionTest {

    private final ExtensionContext mockExtensionContext = mock(ExtensionContext.class);
    private final RetryTest mockRetryTest = mock(RetryTest.class);
    private final Object mockTestInstance = new Object();
    private final Method mockTestMethod = mock(Method.class);
    private RetryExtension retryExtension;

    @BeforeEach
    void beforeEach() {
        retryExtension = new RetryExtension();
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.of(mockTestMethod));
        when(mockExtensionContext.getRequiredTestInstance()).thenReturn(mockTestInstance);
        when(mockTestMethod.getAnnotation(RetryTest.class)).thenReturn(mockRetryTest);
    }

    @Test
    void noRetryAnnotation() throws Throwable {
        when(mockTestMethod.getAnnotation(RetryTest.class)).thenReturn(null);
        var initialException = new RuntimeException("Initial failure");

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.handleTestExecutionException(mockExtensionContext, initialException));

        assertSame(initialException, thrown);
        verify(mockTestMethod, never()).invoke(any(), any());
    }

    @Test
    void noTestMethod() {
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.empty());
        var initialException = new RuntimeException("Initial failure");

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.handleTestExecutionException(mockExtensionContext, initialException));

        assertSame(initialException, thrown);
        verifyNoInteractions(mockTestMethod);
    }

    @Test
    void retryExhausted() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);

        var runtimeException = new RuntimeException("Simulated failure");
        doThrow(new InvocationTargetException(runtimeException)).when(mockTestMethod).invoke(mockTestInstance);

        var thrown = assertThrows(InvocationTargetException.class, () ->
                retryExtension.handleTestExecutionException(mockExtensionContext, new RuntimeException("Initial failure")));

        assertSame(runtimeException, thrown.getTargetException());
        verify(mockTestMethod, times(2)).invoke(mockTestInstance);
    }

    @Test
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    void retrySuccess() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);

        var callCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            if (callCount.incrementAndGet() < 2) {
                throw new InvocationTargetException(new RuntimeException("Simulated failure"));
            }
            return null;
        }).when(mockTestMethod).invoke(mockTestInstance);

        assertDoesNotThrow(() -> retryExtension.handleTestExecutionException(mockExtensionContext, new RuntimeException("Initial failure")));

        verify(mockTestMethod, times(2)).invoke(mockTestInstance);
        assertEquals(2, callCount.get());
    }

    @Test
    void retrySuccessWithCheckedException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);

        var callCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            if (callCount.incrementAndGet() < 2) {
                throw new InvocationTargetException(new IOException("Simulated failure"));
            }
            return null;
        }).when(mockTestMethod).invoke(mockTestInstance);

        assertDoesNotThrow(() -> retryExtension.handleTestExecutionException(mockExtensionContext, new RuntimeException("Initial failure")));

        verify(mockTestMethod, times(2)).invoke(mockTestInstance);
        assertEquals(2, callCount.get());
    }

    @Test
    void retryWithCheckedException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);

        var checkedException = new IOException("Simulated failure");
        doThrow(new InvocationTargetException(checkedException)).when(mockTestMethod).invoke(mockTestInstance);

        var thrown = assertThrows(InvocationTargetException.class, () ->
                retryExtension.handleTestExecutionException(mockExtensionContext, new RuntimeException("Initial failure")));

        assertSame(checkedException, thrown.getTargetException());
        verify(mockTestMethod, times(2)).invoke(mockTestInstance);
    }

    @Test
    void retryWithDelayExhausted() throws Throwable {
        when(mockRetryTest.value()).thenReturn(2);
        when(mockRetryTest.delay()).thenReturn(1);

        var runtimeException = new RuntimeException("Simulated failure");
        doThrow(new InvocationTargetException(runtimeException)).when(mockTestMethod).invoke(mockTestInstance);

        var startTime = System.currentTimeMillis();
        var thrown = assertThrows(InvocationTargetException.class, () ->
                retryExtension.handleTestExecutionException(mockExtensionContext, new RuntimeException("Initial failure")));
        var duration = System.currentTimeMillis() - startTime;

        assertSame(runtimeException, thrown.getTargetException());
        verify(mockTestMethod, times(1)).invoke(mockTestInstance);
        assertTrue(duration >= 1000, "Expected delay of at least 1000ms, but was " + duration + "ms");
    }

    @Test
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    void retryWithDelaySuccess() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        when(mockRetryTest.delay()).thenReturn(1);

        var callCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (callCount.incrementAndGet() < 2) {
                throw new InvocationTargetException(new RuntimeException("Simulated failure"));
            }
            return null;
        }).when(mockTestMethod).invoke(mockTestInstance);

        var startTime = System.currentTimeMillis();
        assertDoesNotThrow(() -> retryExtension.handleTestExecutionException(mockExtensionContext, new RuntimeException("Initial failure")));
        var duration = System.currentTimeMillis() - startTime;

        verify(mockTestMethod, times(2)).invoke(mockTestInstance);
        assertTrue(duration >= 1000, "Expected delay of at least 1000ms, but was " + duration + "ms");
    }

    @Test
    void retryWithInterruptedDelay() {
        when(mockRetryTest.value()).thenReturn(2);
        when(mockRetryTest.delay()).thenReturn(5); // 5 seconds

        var initialException = new RuntimeException("Initial");

        // Interrupt the thread during the delay
        var testThread = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(500); // Wait for the extension to enter the delay
                testThread.interrupt();
            } catch (InterruptedException ignored) {
                // ignore
            }
        }).start();

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.handleTestExecutionException(mockExtensionContext, initialException));

        // Verify the original exception was thrown with the InterruptedException as a suppressed exception
        assertSame(initialException, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertInstanceOf(InterruptedException.class, thrown.getSuppressed()[0]);

        // Verify that the interrupted status of the thread is true
        assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted");
        // Clear the interrupted status for more tests
        var ignored = Thread.interrupted();
    }

    @Test
    void retryWithNonInvocationTargetException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);

        var runtimeException = new IllegalArgumentException("Simulated direct failure");
        doThrow(runtimeException).when(mockTestMethod).invoke(mockTestInstance);

        var thrown = assertThrows(IllegalArgumentException.class, () ->
                retryExtension.handleTestExecutionException(mockExtensionContext,
                        new IllegalArgumentException("Initial failure")));

        assertSame(runtimeException, thrown);
        verify(mockTestMethod, times(2)).invoke(mockTestInstance);
    }
}
