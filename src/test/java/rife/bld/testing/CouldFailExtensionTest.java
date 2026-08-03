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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CouldFailExtension Unit Tests")
@SuppressWarnings("EmptyMethod")
class CouldFailExtensionTest {

    @CouldFail
    @SuppressWarnings("PMD.PublicMemberInNonPublicType")
    public void methodWithCouldFail() {
        // @CouldFail with no withExceptions
    }

    @CouldFail(withExceptions = IOException.class)
    @SuppressWarnings("PMD.PublicMemberInNonPublicType")
    public void methodWithSpecificException() {
        // @CouldFail with specific exception type
    }

    @SuppressWarnings("PMD.PublicMemberInNonPublicType")
    public void methodWithoutAnnotation() {
        // No @CouldFail annotation
    }

    @Test
    @DisplayName("Extension accepts exception in withExceptions list")
    void extensionAcceptsMatchingException() throws Exception {
        var extension = new CouldFailExtension();

        var context = mock(ExtensionContext.class);
        var testMethod = CouldFailExtensionTest.class.getMethod("methodWithSpecificException");
        when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
        when(context.getTestClass()).thenReturn(Optional.of(CouldFailExtensionTest.class));

        // Throw an exception in the withExceptions list
        var testException = new IOException("Accepted exception");

        // Should throw TestAbortedException
        var thrown = assertThrows(org.opentest4j.TestAbortedException.class, () ->
                extension.handleTestExecutionException(context, testException));

        assertTrue(thrown.getMessage().contains("Test marked @CouldFail — accepted as non-fatal"));
        assertSame(testException, thrown.getCause());
    }

    @Test
    @DisplayName("Extension accepts exception when @CouldFail present with no withExceptions")
    void extensionAcceptsWhenAnnotationPresentNoFilter() throws Exception {
        var extension = new CouldFailExtension();

        var context = mock(ExtensionContext.class);
        var testMethod = CouldFailExtensionTest.class.getMethod("methodWithCouldFail");
        when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
        when(context.getTestClass()).thenReturn(Optional.of(CouldFailExtensionTest.class));

        var testException = new IOException("Test exception");

        // Should throw TestAbortedException
        var thrown = assertThrows(org.opentest4j.TestAbortedException.class, () ->
                extension.handleTestExecutionException(context, testException));

        assertTrue(thrown.getMessage().contains("Test marked @CouldFail — accepted as non-fatal"));
        assertSame(testException, thrown.getCause());
    }

    @Test
    @DisplayName("Extension re-throws exception when no @CouldFail annotation present")
    void extensionReThrowsWhenNoAnnotation() throws Exception {
        // Create the extension
        var extension = new CouldFailExtension();

        // Create a mock ExtensionContext with no @CouldFail annotation
        var context = mock(ExtensionContext.class);

        // Mock a test method without @CouldFail annotation
        var testMethod = CouldFailExtensionTest.class.getMethod("methodWithoutAnnotation");
        when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
        when(context.getTestClass()).thenReturn(Optional.of(CouldFailExtensionTest.class));

        // Create a test exception
        var testException = new IOException("Test exception");

        // The extension should re-throw the original exception
        var thrown = assertThrows(IOException.class, () ->
                extension.handleTestExecutionException(context, testException));

        assertSame(testException, thrown, "Original exception should be re-thrown");
    }

    @Test
    @DisplayName("Extension rejects exception not in withExceptions list")
    void extensionRejectsNonMatchingException() throws Exception {
        var extension = new CouldFailExtension();

        var context = mock(ExtensionContext.class);
        var testMethod = CouldFailExtensionTest.class.getMethod("methodWithSpecificException");
        when(context.getTestMethod()).thenReturn(Optional.of(testMethod));
        when(context.getTestClass()).thenReturn(Optional.of(CouldFailExtensionTest.class));

        // Throw an exception NOT in the withExceptions list
        var testException = new IllegalStateException("Wrong exception type");

        // Should re-throw the original exception
        var thrown = assertThrows(IllegalStateException.class, () ->
                extension.handleTestExecutionException(context, testException));

        assertSame(testException, thrown);
    }
}
