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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("PMD.SystemPrintln")
class CaptureOutputExtensionTest {

    // Utility method to set private fields via reflection
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void setPrivateField(Object obj, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        var f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }

    @Test
    void afterEachClosesStreamsWhenNotNull() throws Exception {
        var extension = new CaptureOutputExtension();

        // Mock ExtensionContext and its Store
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);
        when(context.getStore(any())).thenReturn(store);

        // Use mocks for ByteArrayOutputStream to verify close() is called
        var mockOut = mock(ByteArrayOutputStream.class);
        var mockErr = mock(ByteArrayOutputStream.class);

        // Use reflection to set the private fields
        setPrivateField(extension, "capturedOut", mockOut);
        setPrivateField(extension, "capturedErr", mockErr);

        // Set up originalOut and originalErr to avoid NPE on System.setOut/System.setErr
        setPrivateField(extension, "originalOut", System.out);
        setPrivateField(extension, "originalErr", System.err);

        extension.afterEach(context);

        verify(mockOut, times(1)).close();
        verify(mockErr, times(1)).close();
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void afterEachRestoresStreamsAndClosesStreams() throws Exception {
        var extension = new CaptureOutputExtension();

        // Mock ExtensionContext and its Store
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);
        when(context.getStore(any())).thenReturn(store);

        // Save original streams
        var originalOut = System.out;
        var originalErr = System.err;

        try {
            extension.beforeEach(context);

            // After beforeEach, System.out and System.err are not original
            assertNotSame(System.out, originalOut);
            assertNotSame(System.err, originalErr);

            extension.afterEach(context);

            // After afterEach, System.out and System.err should be restored
            assertSame(System.out, originalOut);
            assertSame(System.err, originalErr);
        } finally {
            // Ensure restoration if something goes wrong
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    void afterEachWithNullStreamsDoesNotThrow() throws Exception {
        var extension = new CaptureOutputExtension();

        // Mock ExtensionContext and its Store
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);
        when(context.getStore(any())).thenReturn(store);

        // Set capturedOut and capturedErr to null with reflection
        setPrivateField(extension, "capturedOut", null);
        setPrivateField(extension, "capturedErr", null);

        // Set up originalOut and originalErr to avoid NPE on System.setOut/System.setErr
        setPrivateField(extension, "originalOut", System.out);
        setPrivateField(extension, "originalErr", System.err);

        // Should not throw any exception
        assertDoesNotThrow(() -> extension.afterEach(context));
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void beforeEachRedirectsStreamsAndStoresCapturedOutput() {
        var extension = new CaptureOutputExtension();

        // Mock ExtensionContext and its Store
        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);
        when(context.getStore(any())).thenReturn(store);

        // Save original streams
        var originalOut = System.out;
        var originalErr = System.err;

        try {
            extension.beforeEach(context);

            // After beforeEach, System.out and System.err should not be original
            assertNotSame(System.out, originalOut);
            assertNotSame(System.err, originalErr);

            // Should be PrintStream backed by ByteArrayOutputStream
            System.out.println("HelloOut");
            System.err.println("HelloErr");

            // The captured output should have been stored in the ExtensionContext's Store
            verify(store).put(eq("capturedOutput"), any(CapturedOutput.class));
        } finally {
            // Restore original streams for other tests
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    void resolveParameterReturnsCapturedOutputFromStore() {
        var extension = new CaptureOutputExtension();

        var context = mock(ExtensionContext.class);
        var store = mock(ExtensionContext.Store.class);

        var output = new CapturedOutput(
                new ByteArrayOutputStream(), new ByteArrayOutputStream()
        );
        when(store.get(eq("capturedOutput"), eq(CapturedOutput.class))).thenReturn(output);
        when(context.getStore(any())).thenReturn(store);

        var paramContext = mock(ParameterContext.class);

        var resolved = extension.resolveParameter(paramContext, context);
        assertSame(output, resolved);
    }

    @Test
    void supportsParameterOnlyForCapturedOutput() throws Exception {
        var extension = new CaptureOutputExtension();

        // Mock ParameterContext for CapturedOutput
        var paramContext = mock(ParameterContext.class);

        // For CapturedOutput parameter
        class Dummy {

            @SuppressWarnings({"unused", "EmptyMethod"})
            void method(CapturedOutput output) {
                // no-op
            }
        }
        var method = Dummy.class.getDeclaredMethod("method", CapturedOutput.class);
        var parameter = method.getParameters()[0];
        when(paramContext.getParameter()).thenReturn(parameter);

        assertTrue(extension.supportsParameter(paramContext, mock(ExtensionContext.class)));

        // For a non-CapturedOutput parameter
        class Dummy2 {

            @SuppressWarnings({"unused", "EmptyMethod"})
            void method(String output) {
                // no-op
            }
        }
        var method2 = Dummy2.class.getDeclaredMethod("method", String.class);
        var parameter2 = method2.getParameters()[0];
        when(paramContext.getParameter()).thenReturn(parameter2);

        assertFalse(extension.supportsParameter(paramContext, mock(ExtensionContext.class)));
    }
}