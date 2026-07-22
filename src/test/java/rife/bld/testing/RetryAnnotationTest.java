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
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.*;

class RetryAnnotationTest {

    // Helper class with various annotation configurations for testing
    @SuppressWarnings({"EmptyMethod", "PMD.DetachedTestCase", "PMD.JUnitJupiterTestNoPrivateModifier"})
    private static final class TestMethodsWithAnnotations {

        @RetryTest(value = 5, name = "Custom Name")
        void methodWithCustomValues() {
            // Method with custom values
        }

        @RetryTest
        void methodWithDefaultValues() {
            // Method with default values
        }

        @RetryTest(name = "")
        @SuppressWarnings("DefaultAnnotationParam")
        void methodWithEmptyName() {
            // Method with an explicitly empty name
        }

        @RetryTest(100)
        void methodWithLargeRetryCount() {
            // Method with a large retry count
        }

        @RetryTest(name = "This is a very long test name that should still work correctly")
        void methodWithLongName() {
            // Method with a very long name
        }

        @RetryTest(name = "Only Name")
        void methodWithOnlyCustomName() {
            // Method with only a custom name
        }

        @RetryTest(7)
        void methodWithOnlyCustomRetryCount() {
            // Method with only custom retry count
        }

        @Test
        @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
        void methodWithOtherAnnotation() {
            // Method with different annotation
        }

        @RetryTest(name = "Test with @#$%^&*() characters!")
        void methodWithSpecialCharactersInName() {
            // Method with special characters in the name
        }

        @RetryTest(name = "テスト 🚀 Тест")
        void methodWithUnicodeInName() {
            // Method with Unicode characters in the name
        }

        @RetryTest(0)
        void methodWithZeroRetryCount() {
            // Method with zero-retry count
        }

        void methodWithoutRetryTest() {
            // Method without any annotation
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
        void hasCorrectTargetElement() {
            var annotation = RetryTest.class.getAnnotation(Target.class);

            assertNotNull(annotation);
            assertEquals(1, annotation.value().length);
            assertEquals(ElementType.METHOD, annotation.value()[0]);
        }

        @Test
        void hasExtendWithAnnotation() {
            var annotation = RetryTest.class.getAnnotation(ExtendWith.class);

            assertNotNull(annotation);
            assertEquals(1, annotation.value().length);
            assertEquals(RetryExtension.class, annotation.value()[0]);
        }
    }

    @Nested
    @DisplayName("Annotation Presence Detection")
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
        }

        @Test
        void emptyName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithEmptyName");
            var annotation = testMethod.getAnnotation(RetryTest.class);

            assertNotNull(annotation);
            assertEquals(3, annotation.value()); // default value
            assertEquals("", annotation.name());
        }

        @Test
        void largeRetryCount() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithLargeRetryCount");
            var annotation = testMethod.getAnnotation(RetryTest.class);

            assertNotNull(annotation);
            assertEquals(100, annotation.value());
            assertEquals("", annotation.name()); // default value
        }

        @Test
        void longName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithLongName");
            var annotation = testMethod.getAnnotation(RetryTest.class);

            assertNotNull(annotation);
            assertEquals(3, annotation.value()); // default value
            assertEquals("This is a very long test name that should still work correctly", annotation.name());
        }

        @Test
        void onlyCustomName() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithOnlyCustomName");
            var annotation = testMethod.getAnnotation(RetryTest.class);

            assertNotNull(annotation);
            assertEquals(3, annotation.value()); // default value
            assertEquals("Only Name", annotation.name());
        }

        @Test
        void onlyCustomRetryCount() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithOnlyCustomRetryCount");
            var annotation = testMethod.getAnnotation(RetryTest.class);

            assertNotNull(annotation);
            assertEquals(7, annotation.value());
            assertEquals("", annotation.name()); // default value
        }

        @Test
        void zeroRetryCount() throws NoSuchMethodException {
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithZeroRetryCount");
            var annotation = testMethod.getAnnotation(RetryTest.class);

            assertNotNull(annotation);
            assertEquals(0, annotation.value());
            assertEquals("", annotation.name()); // default value
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
            assertEquals("", annotation1.name());
            assertEquals("Custom Name", annotation2.name());
        }

        @Test
        void withSpecialCharactersInName() throws NoSuchMethodException {
            // Given
            var testMethod = TestMethodsWithAnnotations.class.getDeclaredMethod("methodWithSpecialCharactersInName");
            var annotation = testMethod.getAnnotation(RetryTest.class);

            // Then
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
}