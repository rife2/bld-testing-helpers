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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class RetryAnnotationTest {

    // Helper class with various annotation configurations for testing
    @SuppressWarnings({"EmptyMethod", "PMD.DetachedTestCase", "PMD.JUnitJupiterTestNoPrivateModifier",
            "DefaultAnnotationParam"})
    private static final class TestMethodsWithAnnotations {

        @RetryTest(value = 5, name = "Custom Name")
        void methodWithCustomValues() {
            // no-op
        }

        @RetryTest
        void methodWithDefaultValues() {
            // no-op
        }

        @RetryTest(value = 3, delay = 2)
        void methodWithDelay() {
            // no-op
        }

        @RetryTest(name = "")
        @SuppressWarnings("DefaultAnnotationParam")
        void methodWithEmptyName() {
            // no-op
        }

        @RetryTest(value = 2, delay = 1, name = "Full Config", withExceptions = IOException.class)
        void methodWithFullConfig() {
            // no-op
        }

        @RetryTest(100)
        void methodWithLargeRetryCount() {
            // no-op
        }

        @RetryTest(name = "This is a very long test name that should still work correctly")
        void methodWithLongName() {
            // no-op
        }

        @RetryTest(value = 3, withExceptions = {SocketTimeoutException.class, IOException.class})
        void methodWithMultipleExceptionFilters() {
            // no-op
        }

        @RetryTest(name = "Only Name")
        void methodWithOnlyCustomName() {
            // no-op
        }

        @RetryTest(7)
        void methodWithOnlyCustomRetryCount() {
            // no-op
        }

        @Test
        @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
        void methodWithOtherAnnotation() {
            // no-op
        }

        @RetryTest(value = 3, withExceptions = IOException.class)
        void methodWithSingleExceptionFilter() {
            // no-op
        }

        @RetryTest(name = "Test with @#$%^&*() characters!")
        void methodWithSpecialCharactersInName() {
            // no-op
        }

        @RetryTest(name = "テスト 🚀 Тест")
        void methodWithUnicodeInName() {
            // no-op
        }

        @RetryTest(0)
        void methodWithZeroRetryCount() {
            // no-op
        }

        void methodWithoutRetryTest() {
            // no-op
        }
    }

    @Nested
    @DisplayName("Annotation Meta-Properties")
    class AnnotationMetaProperties {

        @Test
        void hasCorrectRetentionPolicy() {
            var annotation = RetryTest.class.getAnnotation(Retention.class);
            assertNotNull(annotation);
            assertEquals(RetentionPolicy.RUNTIME, annotation.value());
        }

        @Test
        void hasCorrectTargetElements() {
            var annotation = RetryTest.class.getAnnotation(Target.class);
            assertNotNull(annotation);
            assertEquals(2, annotation.value().length);
            assertEquals(ElementType.METHOD, annotation.value()[0]);
            assertEquals(ElementType.ANNOTATION_TYPE, annotation.value()[1]);
        }

        @Test
        @DisplayName("has @Execution(SAME_THREAD) - fixes parallel execution bug")
        void hasExecutionSameThreadAnnotation() {
            var annotation = RetryTest.class.getAnnotation(Execution.class);
            assertNotNull(annotation, "@RetryTest must be meta-annotated with @Execution to prevent concurrent BODY executions");
            assertEquals(ExecutionMode.SAME_THREAD, annotation.value(),
                    "Without SAME_THREAD, TestTemplate provides all N contexts upfront and JUnit runs them concurrently when parallel.enabled=true");
        }

        @Test
        void hasExtendWithAnnotation() {
            var annotation = RetryTest.class.getAnnotation(ExtendWith.class);
            assertNotNull(annotation);
            assertEquals(1, annotation.value().length);
            assertEquals(RetryExtension.class, annotation.value()[0]);
        }

        @Test
        @DisplayName("has @TestTemplate - required for TestTemplateInvocationContextProvider")
        void hasTestTemplateAnnotation() {
            var annotation = RetryTest.class.getAnnotation(TestTemplate.class);
            assertNotNull(annotation, "@RetryTest must be meta-annotated with @TestTemplate");
        }
    }

    @Nested
    @DisplayName("Annotation Presence Detection")
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    class AnnotationPresenceDetection {

        @Test
        void methodWithOtherAnnotationDoesNotHaveRetryTest() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithOtherAnnotation");
            assertFalse(testMethod.isAnnotationPresent(RetryTest.class));
            assertTrue(testMethod.isAnnotationPresent(org.junit.jupiter.api.Test.class));
        }

        @Test
        void methodWithRetryTestIsAnnotationPresent() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithDefaultValues");
            assertTrue(testMethod.isAnnotationPresent(RetryTest.class));
        }

        @Test
        void methodWithoutRetryTestIsNotAnnotationPresent() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithoutRetryTest");
            assertFalse(testMethod.isAnnotationPresent(RetryTest.class));
        }
    }

    @Nested
    @DisplayName("Annotation Values")
    class AnnotationValues {

        @Test
        void customValues() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithCustomValues");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(5, annotation.value());
            assertEquals("Custom Name", annotation.name());
        }

        @Test
        void defaultValues() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithDefaultValues");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(3, annotation.value());
            assertEquals("", annotation.name());
            assertEquals(0, annotation.delay(), "default delay must be 0");
            assertEquals(0, annotation.withExceptions().length, "default withExceptions must be empty = retry on any");
        }

        @Test
        void delayAttribute() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithDelay");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(2, annotation.delay());
        }

        @Test
        void emptyName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithEmptyName");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(3, annotation.value());
            assertEquals("", annotation.name());
        }

        @Test
        void fullConfig() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithFullConfig");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(2, annotation.value());
            assertEquals(1, annotation.delay());
            assertEquals("Full Config", annotation.name());
            assertEquals(1, annotation.withExceptions().length);
        }

        @Test
        void largeRetryCount() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithLargeRetryCount");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(100, annotation.value());
        }

        @Test
        void longName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithLongName");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals("This is a very long test name that should still work correctly", annotation.name());
        }

        @Test
        void multipleExceptionFilters() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithMultipleExceptionFilters");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(2, annotation.withExceptions().length);
        }

        @Test
        void onlyCustomName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithOnlyCustomName");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(3, annotation.value());
            assertEquals("Only Name", annotation.name());
        }

        @Test
        void onlyCustomRetryCount() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithOnlyCustomRetryCount");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(7, annotation.value());
        }

        @Test
        void singleExceptionFilter() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithSingleExceptionFilter");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(1, annotation.withExceptions().length);
            assertEquals(IOException.class, annotation.withExceptions()[0]);
        }

        @Test
        void zeroRetryCount() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithZeroRetryCount");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals(0, annotation.value());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation")
    class EdgeCasesAndValidation {

        @Test
        void canHaveMultipleOnSameClass() throws NoSuchMethodException {
            var method1 = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithDefaultValues");
            var method2 = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithCustomValues");
            assertTrue(method1.isAnnotationPresent(RetryTest.class));
            assertTrue(method2.isAnnotationPresent(RetryTest.class));
            var annotation1 = method1.getAnnotation(RetryTest.class);
            var annotation2 = method2.getAnnotation(RetryTest.class);
            assertEquals(3, annotation1.value());
            assertEquals(5, annotation2.value());
        }

        @Test
        void withSpecialCharactersInName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithSpecialCharactersInName");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals("Test with @#$%^&*() characters!", annotation.name());
        }

        @Test
        void withUnicodeInName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithUnicodeInName");
            var annotation = testMethod.getAnnotation(RetryTest.class);
            assertNotNull(annotation);
            assertEquals("テスト 🚀 Тест", annotation.name());
        }
    }

    @Nested
    @DisplayName("New Changes - Parallel and Short-circuit fixes")
    class NewChangesValidation {

        @Test
        @DisplayName("Display name should include invocation index to avoid collisions")
        void displayNameShouldIncludeIndex() throws Exception {
            var method = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithDefaultValues");
            var retry = method.getAnnotation(RetryTest.class);
            assertNotNull(retry);

            // Build a minimal ExtensionContext mock that returns this method
            var mockContext = org.mockito.Mockito.mock(org.junit.jupiter.api.extension.ExtensionContext.class);
            org.mockito.Mockito.when(mockContext.getRequiredTestMethod()).thenReturn(method);
            org.mockito.Mockito.when(mockContext.getTestMethod()).thenReturn(java.util.Optional.of(method));

            var extension = new RetryExtension();
            var contexts = extension.provideTestTemplateInvocationContexts(mockContext).toList();

            assertEquals(retry.value(), contexts.size(), "should provide N contexts");

            var displayName1 = contexts.get(0).getDisplayName(1);
            var displayName2 = contexts.get(1).getDisplayName(2);

            assertTrue(displayName1.contains("[1/"), "display name should contain [index/max]: got " + displayName1);
            assertTrue(displayName2.contains("[2/"), "display name should contain [index/max]: got " + displayName2);
            assertNotEquals(displayName1, displayName2, "each invocation should have distinct display name");
            assertTrue(displayName1.startsWith(method.getName()), "should start with method name or custom name");
        }

        @Test
        @DisplayName("RetryExtension.RetryState must be safe for concurrent visibility")
        void retryStateMustBeSafe() throws Exception {
            Class<?> stateClass = null;
            for (var inner : RetryExtension.class.getDeclaredClasses()) {
                if ("RetryState".equals(inner.getSimpleName())) {
                    stateClass = inner;
                    break;
                }
            }
            assertNotNull(stateClass);
            var attemptField = stateClass.getDeclaredField("attempt");
            // Must be AtomicInteger to avoid PMD Non-atomic operation on volatile
            assertTrue(java.util.concurrent.atomic.AtomicInteger.class.isAssignableFrom(attemptField.getType()),
                    "attempt should be AtomicInteger, not volatile int - volatile++ is non-atomic");
            var finishedField = stateClass.getDeclaredField("finished");
            assertTrue(java.lang.reflect.Modifier.isVolatile(finishedField.getModifiers())
                            || java.util.concurrent.atomic.AtomicBoolean.class.isAssignableFrom(finishedField.getType()),
                    "finished should be volatile or atomic");
        }
    }
}
