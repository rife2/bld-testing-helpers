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
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.DoNotUseThreads", "PMD.AvoidThrowingRawExceptionTypes"})
class RetryExtensionTest {

    private final ExtensionContext mockExtensionContext = mock(ExtensionContext.class);
    @SuppressWarnings("unchecked")
    private final Invocation<Void> mockInvocation = mock(Invocation.class);
    @SuppressWarnings("unchecked")
    private final ReflectiveInvocationContext<Method> mockInvocationContext = mock(ReflectiveInvocationContext.class);
    private final RetryTest mockRetryTest = mock(RetryTest.class);
    private final Method mockTestMethod = mock(Method.class);
    private RetryExtension retryExtension;

    @BeforeEach
    void beforeEach() {
        retryExtension = new RetryExtension();
        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.of(mockTestMethod));
        when(mockExtensionContext.getRequiredTestMethod()).thenReturn(mockTestMethod);
        when(mockTestMethod.getAnnotation(RetryTest.class)).thenReturn(mockRetryTest);
        when(mockTestMethod.isAnnotationPresent(RetryTest.class)).thenReturn(true);
        when(mockRetryTest.name()).thenReturn("");
        when(mockRetryTest.value()).thenReturn(3);
        when(mockRetryTest.delay()).thenReturn(0);
        //noinspection unchecked
        when(mockRetryTest.withExceptions()).thenReturn(new Class[0]);
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

        var contexts = retryExtension.provideTestTemplateInvocationContexts(mockExtensionContext).toList();
        assertEquals(1, contexts.size());
        assertEquals("testFoo", contexts.get(0).getDisplayName(1));

        when(mockRetryTest.name()).thenReturn("custom name");
        contexts = retryExtension.provideTestTemplateInvocationContexts(mockExtensionContext).toList();
        assertEquals("custom name", contexts.get(0).getDisplayName(1));
    }

    @Test
    void retryExhausted() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var runtimeException = new RuntimeException("Simulated failure");
        doThrow(runtimeException).when(mockInvocation).proceed();

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        assertSame(runtimeException, thrown);
        verify(mockInvocation, times(3)).proceed();
    }

    @Test
    void retrySuccess() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var callCount = new AtomicInteger(0);

        doAnswer(inv -> {
            if (callCount.incrementAndGet() < 2) {
                throw new RuntimeException("Simulated failure");
            }
            return null;
        }).when(mockInvocation).proceed();

        assertDoesNotThrow(() ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        verify(mockInvocation, times(2)).proceed();
    }

    @Test
    void retrySuccessWithCheckedException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var callCount = new AtomicInteger(0);

        doAnswer(inv -> {
            if (callCount.incrementAndGet() < 2) {
                throw new IOException("Simulated failure");
            }
            return null;
        }).when(mockInvocation).proceed();

        assertDoesNotThrow(() ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        verify(mockInvocation, times(2)).proceed();
    }

    @Test
    void retryWithCauseChain() throws Throwable {
        when(mockRetryTest.value()).thenReturn(1);
        var root = new IOException("root");
        var wrapper = new RuntimeException(null, root);
        doThrow(wrapper).when(mockInvocation).proceed();

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        assertSame(wrapper, thrown);
    }

    @Test
    void retryWithCheckedException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        var checkedException = new IOException("Simulated failure");
        doThrow(checkedException).when(mockInvocation).proceed();

        var thrown = assertThrows(IOException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        assertSame(checkedException, thrown);
        verify(mockInvocation, times(3)).proceed();
    }

    @Test
    void retryWithDelayExhausted() throws Throwable {
        when(mockRetryTest.value()).thenReturn(2);
        when(mockRetryTest.delay()).thenReturn(1);
        var runtimeException = new RuntimeException("Simulated failure");
        doThrow(runtimeException).when(mockInvocation).proceed();

        var startTime = System.currentTimeMillis();
        assertThrows(RuntimeException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));
        var duration = System.currentTimeMillis() - startTime;

        verify(mockInvocation, times(2)).proceed();
        assertTrue(duration >= 1000, "Expected delay >= 1000ms, was " + duration);
    }

    @Test
    void retryWithDelaySuccess() throws Throwable {
        when(mockRetryTest.value()).thenReturn(3);
        when(mockRetryTest.delay()).thenReturn(1);
        var callCount = new AtomicInteger(0);
        doAnswer(inv -> {
            if (callCount.incrementAndGet() < 2) {
                throw new RuntimeException("Simulated failure");
            }
            return null;
        }).when(mockInvocation).proceed();

        var startTime = System.currentTimeMillis();
        assertDoesNotThrow(() ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));
        var duration = System.currentTimeMillis() - startTime;

        verify(mockInvocation, times(2)).proceed();
        assertTrue(duration >= 1000, "Expected delay >= 1000ms, was " + duration);
    }

    @Test
    void retryWithInterruptedDelay() throws Throwable {
        when(mockRetryTest.value()).thenReturn(2);
        when(mockRetryTest.delay()).thenReturn(5);
        var initialException = new RuntimeException("Initial");
        doThrow(initialException).when(mockInvocation).proceed();

        var testThread = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(500);
                testThread.interrupt();
            } catch (InterruptedException ignored) {
            }
        }).start();

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        assertSame(initialException, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertInstanceOf(InterruptedException.class, thrown.getSuppressed()[0]);
        assertTrue(Thread.currentThread().isInterrupted());
        //noinspection ResultOfMethodCallIgnored
        Thread.interrupted(); // clear
    }

    @Test
    void retryWithMatchingCauseInChain() throws Throwable {
        //noinspection unchecked
        when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});
        var root = new IOException("root");
        var wrapper = new RuntimeException(root);
        var callCount = new AtomicInteger(0);
        doAnswer(inv -> {
            if (callCount.incrementAndGet() < 2) {
                throw wrapper;
            }
            return null;
        }).when(mockInvocation).proceed();

        assertDoesNotThrow(() ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        verify(mockInvocation, times(2)).proceed();
    }

    @Test
    void retryWithMatchingException() throws Throwable {
        //noinspection unchecked
        when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});
        var ioEx = new IOException("io");
        doThrow(ioEx).when(mockInvocation).proceed();

        var thrown = assertThrows(IOException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        assertSame(ioEx, thrown);
        verify(mockInvocation, times(3)).proceed(); // retried
    }

    @Test
    void retryWithNoMessageException() throws Throwable {
        when(mockRetryTest.value()).thenReturn(1);
        var noMessageEx = new RuntimeException();
        doThrow(noMessageEx).when(mockInvocation).proceed();

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        assertSame(noMessageEx, thrown);
        verify(mockInvocation, times(1)).proceed();
    }

    @Test
    void retryWithNonMatchingExceptionFailsFast() throws Throwable {
        //noinspection unchecked
        when(mockRetryTest.withExceptions()).thenReturn(new Class[]{IOException.class});
        var runtimeEx = new RuntimeException("other");
        doThrow(runtimeEx).when(mockInvocation).proceed();

        var thrown = assertThrows(RuntimeException.class, () ->
                retryExtension.interceptTestTemplateMethod(mockInvocation, mockInvocationContext, mockExtensionContext));

        assertSame(runtimeEx, thrown);
        verify(mockInvocation, times(1)).proceed(); // no retry
    }

    @Test
    void supportsTestTemplate() {
        assertTrue(retryExtension.supportsTestTemplate(mockExtensionContext));

        when(mockTestMethod.isAnnotationPresent(RetryTest.class)).thenReturn(false);
        assertFalse(retryExtension.supportsTestTemplate(mockExtensionContext));

        when(mockExtensionContext.getTestMethod()).thenReturn(Optional.empty());
        assertFalse(retryExtension.supportsTestTemplate(mockExtensionContext));
    }
}