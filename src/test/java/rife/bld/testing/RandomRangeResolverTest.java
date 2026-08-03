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
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AvoidAccessibilityAlteration"})
class RandomRangeResolverTest {

    // Fake methods for extracting real Parameter objects
    @SuppressWarnings({"EmptyMethod", "unused"})
    static void intPrimitiveParamMethod(int param) {
        // no-op
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    static void integerParamMethod(Integer param) {
        // no-op
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    @RandomRange(min = 1, max = 2)
    static void sampleMethod(int v) {
        // no-op
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    static void stringParamMethod(String param) {
        // no-op
    }

    @Nested
    @DisplayName("Edge Cases and Full Branches")
    class EdgeCases {

        @Test
        void generateRandomValueThrowsIfMinEqualsMaxIsValid() {
            var resolver = new RandomRangeResolver();
            var annotation = new RandomRange() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return RandomRange.class;
                }

                @Override
                public int max() {
                    return 7;
                }

                @Override
                public int min() {
                    return 7;
                }

                @Override
                public int size() {
                    return 0;  // Add this method
                }
            };
            // Should not throw, 7 is a valid value
            var value = invokeGenerateRandomValue(resolver, annotation);
            assertInstanceOf(Integer.class, value);
            assertEquals(7, value);
        }

        // Use reflection to invoke the private method for coverage
        @SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidAccessibilityAlteration"})
        private Object invokeGenerateRandomValue(RandomRangeResolver resolver, RandomRange ann) {
            try {
                var m = RandomRangeResolver.class.getDeclaredMethod("generateRandomValue", RandomRange.class);
                m.setAccessible(true);
                return m.invoke(resolver, ann);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    class FieldAnnotationTests {

        @RandomRange(min = 100, max = 101)
        @SuppressWarnings("unused")
        private static int staticRandomField;

        @RandomRange(min = 21, max = 23)
        @SuppressWarnings("unused")
        int randomField;

        @Nested
        @DisplayName("Field Annotation Coverage")
        class FieldAnnotationCoverage {

            @Mock
            ExtensionContext extensionContext;
            @Mock
            ParameterContext parameterContext;

            FieldAnnotationCoverage() {
                MockitoAnnotations.openMocks(this);
            }

            @Test
            void fieldAnnotationCanBeReadAndHasCorrectValues() throws NoSuchFieldException {
                var fieldAnn = getFieldRandomRangeAnnotation("randomField");
                assertNotNull(fieldAnn);
                assertEquals(21, fieldAnn.min());
                assertEquals(23, fieldAnn.max());

                var staticFieldAnn = getFieldRandomRangeAnnotation("staticRandomField");
                assertNotNull(staticFieldAnn);
                assertEquals(100, staticFieldAnn.min());
                assertEquals(101, staticFieldAnn.max());
            }

            @Test
            void generateRandomValueWorksWithFieldAnnotation()
                    throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
                var fieldAnn = getFieldRandomRangeAnnotation("randomField");
                var resolver = new RandomRangeResolver();

                // Use reflection to access the private method
                var m = RandomRangeResolver.class.getDeclaredMethod("generateRandomValue", RandomRange.class);
                m.setAccessible(true);
                var value = m.invoke(resolver, fieldAnn);

                assertInstanceOf(Integer.class, value);
                int v = (Integer) value;
                assertTrue(v >= 21 && v <= 23);
            }

            private RandomRange getFieldRandomRangeAnnotation(String fieldName) throws NoSuchFieldException {
                var field = FieldAnnotationTests.class.getDeclaredField(fieldName);
                return field.getAnnotation(RandomRange.class);
            }

            @Test
            void supportsParameterReturnsFalseForFieldAnnotationOnly() throws NoSuchMethodException {
                // Initialize mocks for this test
                MockitoAnnotations.openMocks(this);
                // Field-level annotations are not supported for parameter resolution by the resolver.
                // Only method and parameter-level annotations are considered.
                var intParam = RandomRangeResolverTest.class
                        .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                        .getParameters()[0];

                when(parameterContext.getParameter()).thenReturn(intParam);
                when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);
                when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

                var resolver = new RandomRangeResolver();
                assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
            }
        }
    }

    @Nested
    @DisplayName("postProcessTestInstance Coverage")
    class PostProcessTestInstanceCoverage {

        @Test
        @DisplayName("injects using default min/max if not specified")
        void injectsDefaultRangeWhenNotSpecified() throws Exception {
            @SuppressWarnings("unused")
            class TestClass {

                @RandomRange
                private int field;

                int getField() {
                    return field;
                }
            }

            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var value = testInstance.getField();
            assertTrue(value >= 0 && value <= 100);
        }

        @Test
        @DisplayName("injects random int into multiple and inherited fields")
        void injectsMultipleAndInheritedFields() throws Exception {
            //noinspection unused
            class BaseClass {

                @RandomRange(min = 20, max = 30)
                private int baseField;

                int getBaseField() {
                    return baseField;
                }
            }

            //noinspection unused
            class ChildClass extends BaseClass {

                @RandomRange(min = 1, max = 2)
                private int childField;

                int getChildField() {
                    return childField;
                }
            }

            var testInstance = new ChildClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var childVal = testInstance.getChildField();
            var baseVal = testInstance.getBaseField();
            assertTrue(childVal >= 1 && childVal <= 2, "childField: " + childVal);
            assertTrue(baseVal >= 20 && baseVal <= 30, "baseField: " + baseVal);
        }

        @Test
        @DisplayName("inject random int into Integer field")
        void injectsRandomIntIntoIntegerField() throws Exception {
            @SuppressWarnings("unused")
            class TestClass {

                @RandomRange(min = 10, max = 15)
                private Integer field;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var value = testInstance.field;
            assertTrue(value >= 10 && value <= 15, "Injected value: " + value);
        }

        @Test
        @DisplayName("injects random int into private field")
        void injectsRandomIntPrivateField() throws Exception {
            //noinspection unused
            class TestClass {

                @RandomRange(min = 10, max = 15)
                private int field;

                int getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var value = testInstance.getField();
            assertTrue(value >= 10 && value <= 15, "Injected value: " + value);
        }

        @Test
        @DisplayName("injects value into private field even if originally inaccessible")
        void injectsValueIntoPrivateFieldRegardlessOfAccessibility() throws Exception {
            //noinspection unused
            class TestClass {

                @RandomRange(min = 1, max = 2)
                private int field;

                int getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            var field = TestClass.class.getDeclaredField("field");
            field.setAccessible(false);

            resolver.postProcessTestInstance(testInstance, null);

            // The field should have a value in range, regardless of accessibility state
            var value = testInstance.getField();
            assertTrue(value == 1 || value == 2, "Injected value: " + value);
        }

        @Test
        @DisplayName("injects when min equals max")
        void injectsWhenMinEqualsMax() throws Exception {
            //noinspection unused
            class TestClass {

                @RandomRange(min = 5, max = 5)
                private int field;

                int getField() {
                    return field;
                }
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            assertEquals(5, testInstance.getField());
        }

        @Test
        @DisplayName("no-op for classes with no annotated fields")
        void noAnnotatedFieldsNoOp() throws Exception {
            @SuppressWarnings("unused")
            class TestClass {

                private int a;
                private String b;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            assertEquals(0, testInstance.a);
            assertNull(testInstance.b);
        }

        @Test
        @DisplayName("skips static fields and non-int fields")
        void skipsStaticAndNonIntFields() throws Exception {
            class TestClass {

                @RandomRange
                @SuppressWarnings("unused")
                private static int staticInt;

                @RandomRange
                @SuppressWarnings("unused")
                private int injected;

                @SuppressWarnings("unused")
                private String notAnInt;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            resolver.postProcessTestInstance(testInstance, null);

            var injectedField = TestClass.class.getDeclaredField("injected");
            injectedField.setAccessible(true);
            var injectedVal = injectedField.getInt(testInstance);
            assertTrue(injectedVal >= 0 && injectedVal <= 100);

            var staticField = TestClass.class.getDeclaredField("staticInt");
            staticField.setAccessible(true);
            assertEquals(0, staticField.getInt(null)); // Static field should not be injected

            var notAnIntField = TestClass.class.getDeclaredField("notAnInt");
            notAnIntField.setAccessible(true);
            assertNull(notAnIntField.get(testInstance));
        }

        @Test
        @DisplayName("throws if min > max")
        void throwsIfMinGreaterThanMax() {
            class TestClass {

                @RandomRange(min = 10, max = 5)
                @SuppressWarnings("unused")
                private int failField;
            }
            var testInstance = new TestClass();
            var resolver = new RandomRangeResolver();

            var ex = assertThrows(Exception.class, () -> resolver.postProcessTestInstance(testInstance, null));
            assertTrue(ex.getMessage().toLowerCase().contains("min") || ex.getMessage().toLowerCase().contains("greater"));
        }
    }

    @Nested
    @DisplayName("Range Size Tests")
    class RandomRangeSizeTests {

        // Fake methods for extracting real Parameter objects
        @SuppressWarnings({"EmptyMethod", "unused"})
        static void listIntegerParamMethod(List<Integer> param) {
            // no-op
        }

        @SuppressWarnings({"EmptyMethod", "unused"})
        static void listStringParamMethod(List<String> param) {
            // no-op
        }

        @SuppressWarnings({"EmptyMethod", "unused"})
        static void setIntegerParamMethod(Set<Integer> param) {
            // no-op
        }

        @Nested
        @DisplayName("Edge Cases for Collections")
        class CollectionEdgeCases {

            @Test
            @DisplayName("generates empty List when size is 0")
            void generatesEmptyListWhenSizeIsZero() throws Exception {
                //noinspection unused
                class TestClass {

                    @RandomRange(min = 1, max = 10)
                    private List<Integer> listField;

                    List<Integer> getListField() {
                        return listField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                var list = testInstance.getListField();
                assertNotNull(list);
                assertEquals(0, list.size());
            }

            @Test
            @DisplayName("generates List with duplicate values allowed")
            void generatesListWithDuplicates() throws Exception {
                //noinspection unused
                class TestClass {

                    @RandomRange(size = 10, min = 1, max = 3)  // only 3 possible values
                    private List<Integer> listField;

                    List<Integer> getListField() {
                        return listField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                var list = testInstance.getListField();
                assertEquals(10, list.size());
                for (Integer value : list) {
                    assertTrue(value >= 1 && value <= 3);
                }
            }

            @Test
            @DisplayName("generates Set with maximum possible unique values")
            void generatesSetWithMaxUniqueValues() throws Exception {
                //noinspection unused
                class TestClass {

                    @RandomRange(size = 5, min = 1, max = 5)  // exactly 5 possible values
                    private Set<Integer> setField;

                    Set<Integer> getSetField() {
                        return setField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                var set = testInstance.getSetField();
                assertEquals(5, set.size());
                // Should contain all values from 1 to 5
                assertTrue(set.contains(1));
                assertTrue(set.contains(2));
                assertTrue(set.contains(3));
                assertTrue(set.contains(4));
                assertTrue(set.contains(5));
            }

            @Test
            @DisplayName("generates Set with single element range")
            void generatesSetWithSingleElementRange() throws Exception {
                //noinspection unused
                class TestClass {

                    @RandomRange(size = 1, min = 7, max = 7)
                    private Set<Integer> setField;

                    Set<Integer> getSetField() {
                        return setField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                var set = testInstance.getSetField();
                assertEquals(1, set.size());
                assertTrue(set.contains(7));
            }
        }

        @Nested
        @DisplayName("Field Injection for Lists and Sets")
        class FieldInjectionForCollections {

            @Test
            @DisplayName("injects List<Integer> into field")
            void injectsListIntoField() throws Exception {
                //noinspection unused
                class TestClass {

                    @RandomRange(size = 5, min = 10, max = 20)
                    private List<Integer> listField;

                    List<Integer> getListField() {
                        return listField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                var list = testInstance.getListField();
                assertNotNull(list);
                assertEquals(5, list.size());
                for (Integer value : list) {
                    assertTrue(value >= 10 && value <= 20);
                }
            }

            @Test
            @DisplayName("injects List<Integer> with default min/max")
            void injectsListWithDefaults() throws Exception {
                //noinspection unused
                class TestClass {

                    @RandomRange(size = 3)
                    private List<Integer> listField;

                    List<Integer> getListField() {
                        return listField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                var list = testInstance.getListField();
                assertNotNull(list);
                assertEquals(3, list.size());
                for (Integer value : list) {
                    assertTrue(value >= 0 && value <= 100);
                }
            }

            @Test
            @DisplayName("injects Set<Integer> into field")
            void injectsSetIntoField() throws Exception {
                //noinspection unused
                class TestClass {

                    @RandomRange(size = 8, min = 1, max = 50)
                    private Set<Integer> setField;

                    Set<Integer> getSetField() {
                        return setField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                var set = testInstance.getSetField();
                assertNotNull(set);
                assertEquals(8, set.size());
                for (Integer value : set) {
                    assertTrue(value >= 1 && value <= 50);
                }
            }

            @Test
            @DisplayName("skips non-List/Set fields even with size parameter")
            void skipsNonCollectionFieldsWithSize() throws Exception {
                //noinspection unused
                //noinspection unused
                class TestClass {

                    @RandomRange(size = 5, min = 1, max = 10)
                    private int intField;

                    @RandomRange(size = 5, min = 1, max = 10)
                    private List<Integer> listField;

                    int getIntField() {
                        return intField;
                    }

                    List<Integer> getListField() {
                        return listField;
                    }
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                resolver.postProcessTestInstance(testInstance, null);

                // int field should be injected with a single value (size ignored for int)
                var intValue = testInstance.getIntField();
                assertTrue(intValue >= 1 && intValue <= 10);

                // List field should be injected with 5 values
                var list = testInstance.getListField();
                assertNotNull(list);
                assertEquals(5, list.size());
            }

            @Test
            @DisplayName("throws when Set size exceeds possible range")
            void throwsWhenSetSizeExceedsRange() {
                class TestClass {

                    @RandomRange(size = 10, min = 1, max = 5)  // range of 5, requesting 10
                    @SuppressWarnings("unused")
                    private Set<Integer> setField;
                }

                var testInstance = new TestClass();
                var resolver = new RandomRangeResolver();

                assertThrows(IllegalArgumentException.class, () ->
                        resolver.postProcessTestInstance(testInstance, null));
            }
        }

        @Nested
        @DisplayName("List and Set Parameter Resolution")
        class ListAndSetResolution {

            @Mock
            ExtensionContext extensionContext;
            @Mock
            ParameterContext parameterContext;

            ListAndSetResolution() {
                MockitoAnnotations.openMocks(this);
            }

            @Test
            @DisplayName("does not support List<String> parameter type")
            void doesNotSupportListStringParameter() throws Exception {
                var listParam = RandomRangeSizeTests.class
                        .getDeclaredMethod("listStringParamMethod", List.class)
                        .getParameters()[0];

                when(parameterContext.getParameter()).thenReturn(listParam);
                when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);

                var resolver = new RandomRangeResolver();
                assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
            }

            private RandomRange mockAnnotation(int min, int max, int size) {
                return new RandomRange() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return RandomRange.class;
                    }

                    @Override
                    public int max() {
                        return max;
                    }

                    @Override
                    public int min() {
                        return min;
                    }

                    @Override
                    public int size() {
                        return size;
                    }
                };
            }

            @Test
            @DisplayName("resolves List<Integer> with size parameter")
            void resolvesListWithSize() throws Exception {
                var listParam = RandomRangeSizeTests.class
                        .getDeclaredMethod("listIntegerParamMethod", List.class)
                        .getParameters()[0];

                var annotation = mockAnnotation(1, 10, 5);
                when(parameterContext.getParameter()).thenReturn(listParam);
                when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(annotation));

                var resolver = new RandomRangeResolver();
                var result = resolver.resolveParameter(parameterContext, extensionContext);

                assertInstanceOf(List.class, result);
                @SuppressWarnings("unchecked")
                List<Integer> list = (List<Integer>) result;
                assertEquals(5, list.size());
                for (Integer value : list) {
                    assertTrue(value >= 1 && value <= 10);
                }
            }

            @Test
            @DisplayName("resolves Set<Integer> with size parameter")
            void resolvesSetWithSize() throws Exception {
                var setParam = RandomRangeSizeTests.class
                        .getDeclaredMethod("setIntegerParamMethod", Set.class)
                        .getParameters()[0];

                var annotation = mockAnnotation(2, 100, 10);
                when(parameterContext.getParameter()).thenReturn(setParam);
                when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(annotation));

                var resolver = new RandomRangeResolver();
                var result = resolver.resolveParameter(parameterContext, extensionContext);

                assertInstanceOf(Set.class, result);
                @SuppressWarnings("unchecked")
                Set<Integer> set = (Set<Integer>) result;
                assertEquals(10, set.size());
                for (Integer value : set) {
                    assertTrue(value >= 2 && value <= 100);
                }
            }

            @Test
            @DisplayName("supports List<Integer> parameter type")
            void supportsListIntegerParameter() throws Exception {
                var listParam = RandomRangeSizeTests.class
                        .getDeclaredMethod("listIntegerParamMethod", List.class)
                        .getParameters()[0];

                when(parameterContext.getParameter()).thenReturn(listParam);
                when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);

                var resolver = new RandomRangeResolver();
                assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
            }

            @Test
            @DisplayName("supports Set<Integer> parameter type")
            void supportsSetIntegerParameter() throws Exception {
                var setParam = RandomRangeSizeTests.class
                        .getDeclaredMethod("setIntegerParamMethod", Set.class)
                        .getParameters()[0];

                when(parameterContext.getParameter()).thenReturn(setParam);
                when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);

                var resolver = new RandomRangeResolver();
                assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
            }

            @Test
            @DisplayName("throws when requesting more unique values than possible range")
            void throwsWhenSizeExceedsRange() throws Exception {
                var setParam = RandomRangeSizeTests.class
                        .getDeclaredMethod("setIntegerParamMethod", Set.class)
                        .getParameters()[0];

                var annotation = mockAnnotation(1, 5, 10);  // range of 5, requesting 10 unique
                when(parameterContext.getParameter()).thenReturn(setParam);
                when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(annotation));

                var resolver = new RandomRangeResolver();
                assertThrows(IllegalArgumentException.class, () ->
                        resolver.resolveParameter(parameterContext, extensionContext));
            }
        }
    }

    @Nested
    @DisplayName("Resolve Parameter Tests")
    class ResolveParameterTests {

        @Mock
        ExtensionContext extensionContext;
        @Mock
        Method method;
        @Mock
        ParameterContext parameterContext;

        ResolveParameterTests() {
            MockitoAnnotations.openMocks(this);
        }

        private RandomRange mockAnnotation(int min, int max) {
            return new RandomRange() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return RandomRange.class;
                }

                @Override
                public int max() {
                    return max;
                }

                @Override
                public int min() {
                    return min;
                }

                @Override
                public int size() {
                    return 0;
                }
            };
        }

        @Test
        @DisplayName("should not support raw List parameters without type parameter")
        void notSupportRawListParameters() throws Exception {
            var extension = new RandomRangeResolver();

            class TestClass {

                @SuppressWarnings({"unused", "rawtypes", "EmptyMethod"})
                void testMethod(List param) {
                    // no-op
                }
            }

            var method = TestClass.class.getDeclaredMethod("testMethod", List.class);
            var realParameter = method.getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(realParameter);

            var result = extension.supportsParameter(parameterContext, extensionContext);

            assertFalse(result);
        }

        @Test
        void throwsIfMinGreaterThanMax() {
            var paramAnn = mockAnnotation(10, 5);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(paramAnn));
            var resolver = new RandomRangeResolver();
            assertThrows(ParameterResolutionException.class, () ->
                    resolver.resolveParameter(parameterContext, extensionContext));
        }

        @Test
        void usesDefaultRangeIfNoAnnotation() {
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.empty());
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(method));
            when(method.getAnnotation(RandomRange.class)).thenReturn(null);

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 0 && v <= 100);
        }

        @Test
        void usesDefaultRangeIfNoTestMethod() {
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.empty());
            when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 0 && v <= 100);
        }

        @Test
        void usesMethodLevelAnnotation() {
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.empty());

            var methodAnn = mockAnnotation(11, 13);
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(method));
            when(method.getAnnotation(RandomRange.class)).thenReturn(methodAnn);

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 11 && v <= 13);
        }

        @Test
        void usesParameterLevelAnnotation() {
            var paramAnn = mockAnnotation(5, 15);
            when(parameterContext.findAnnotation(RandomRange.class)).thenReturn(Optional.of(paramAnn));

            var resolver = new RandomRangeResolver();
            var value = resolver.resolveParameter(parameterContext, extensionContext);
            assertInstanceOf(Integer.class, value);
            int v = (Integer) value;
            assertTrue(v >= 5 && v <= 15);
        }


    }

    @Nested
    @DisplayName("Supports Parameter Tests")
    class SupportsParameterTests {

        @Mock
        ExtensionContext extensionContext;
        @Mock
        ParameterContext parameterContext;

        SupportsParameterTests() {
            MockitoAnnotations.openMocks(this);
        }

        @Test
        void returnsFalseForNonIntType() throws Exception {
            var stringParam = RandomRangeResolverTest.class
                    .getDeclaredMethod("stringParamMethod", String.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(stringParam);

            var resolver = new RandomRangeResolver();
            assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsFalseIfNoAnnotationsPresent() throws Exception {
            var intParam = RandomRangeResolverTest.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);

            // Use a real method without @RandomRange
            var m = RandomRangeResolverTest.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class);
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(m));

            var resolver = new RandomRangeResolver();
            assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsFalseIfNoTestMethodPresent() throws Exception {
            var intParam = RandomRangeResolverTest.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);
            when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

            var resolver = new RandomRangeResolver();
            assertFalse(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsTrueForMethodLevelAnnotation() throws Exception {
            var intParam = RandomRangeResolverTest.class
                    .getDeclaredMethod("sampleMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(false);

            // Use the real annotated method
            var m = RandomRangeResolverTest.class
                    .getDeclaredMethod("sampleMethod", int.class);
            when(extensionContext.getTestMethod()).thenReturn(Optional.of(m));

            var resolver = new RandomRangeResolver();
            assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsTrueForParameterLevelAnnotation() throws Exception {
            var intParam = RandomRangeResolverTest.class
                    .getDeclaredMethod("intPrimitiveParamMethod", int.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);

            var resolver = new RandomRangeResolver();
            assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
        }

        @Test
        void returnsTrueForParameterLevelAnnotationWithInteger() throws Exception {
            var intParam = RandomRangeResolverTest.class
                    .getDeclaredMethod("integerParamMethod", Integer.class)
                    .getParameters()[0];

            when(parameterContext.getParameter()).thenReturn(intParam);
            when(parameterContext.isAnnotated(RandomRange.class)).thenReturn(true);

            var resolver = new RandomRangeResolver();
            assertTrue(resolver.supportsParameter(parameterContext, extensionContext));
        }
    }
}