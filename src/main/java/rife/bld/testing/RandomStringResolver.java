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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Parameter and field resolver for the {@link RandomString} annotation.
 * <p>
 * This resolver automatically injects random string values into test method parameters that are annotated with
 * {@code @RandomString} or are part of test methods annotated with {@code @RandomString} at the method level.
 *
 * <h3>Supported Types:</h3>
 * <p>
 * <ul>
 *   <li>{@code String} - single random string (when size = 0)</li>
 *   <li>{@code List<String>} - list of random strings (when size > 0)</li>
 *   <li>{@code Set<String>} - set of unique random strings (when size > 0)</li>
 * </ul>
 *
 * <h3>Resolution Priority:</h3>
 * <p>
 * Parameter-level annotation takes precedence over method-level.
 *
 * <h3>Security:</h3>
 * <p>
 * Uses {@link java.security.SecureRandom} for cryptographically strong random number generation.
 *
 * <h3>Default Configuration:</h3>
 * <p>
 * <ul>
 *   <li><strong>Length:</strong> 10 characters</li>
 *   <li><strong>Character Set:</strong> Alphanumeric (A-Z, a-z, 0-9)</li>
 *   <li><strong>Size:</strong> 0 (single string)</li>
 * </ul>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see ParameterResolver
 * @see RandomString
 * @see TestInstancePostProcessor
 * @since 1.0
 */
public class RandomStringResolver implements ParameterResolver, TestInstancePostProcessor {

    /**
     * Processes fields of the test instance annotated with {@link RandomString}.
     * <p>
     * Enables field injection for random strings. Field must be of type {@code String}, {@code List<String>},
     * or {@code Set<String>}. Fields of unsupported types annotated with {@code @RandomString} will cause an
     * {@link IllegalArgumentException} to be thrown.
     *
     * @param testInstance the test class instance
     * @param context      the current extension context
     * @throws IllegalArgumentException if a {@code @RandomString}-annotated field has an unsupported type
     */
    @Override
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @SuppressFBWarnings("RFI_SET_ACCESSIBLE")
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws IllegalAccessException {
        Objects.requireNonNull(testInstance, "testInstance" + TestingUtils.CANNOT_BE_NULL);

        for (var currentClass = testInstance.getClass(); currentClass != Object.class;
             currentClass = currentClass.getSuperclass()) {
            for (var field : currentClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isAnnotationPresent(RandomString.class)) {
                    var annotation = field.getAnnotation(RandomString.class);
                    Object randomValue;

                    if (field.getType() == String.class) {
                        randomValue = TestingUtils.generateRandomString(annotation.length(), annotation.characters());
                    } else if (field.getType() == List.class) {
                        randomValue = generateRandomStringList(annotation.size(), annotation.length(),
                                annotation.characters());
                    } else if (field.getType() == Set.class) {
                        randomValue = generateRandomStringSet(annotation.size(), annotation.length(),
                                annotation.characters());
                    } else {
                        throw new IllegalArgumentException(
                                "Unsupported field type for @RandomString: %s on field '%s'. "
                                        .formatted(field.getType().getName(), field.getName()) +
                                        "Supported types are String, List<String>, and Set<String>.");
                    }

                    field.setAccessible(true);
                    field.set(testInstance, randomValue);
                }
            }
        }
    }

    /**
     * Determines if this resolver can resolve a parameter.
     *
     * @param parameterContext information about the parameter to be resolved
     * @param extensionContext the current extension context
     * @return {@code true} if the parameter can be resolved by this extension, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var parameterType = parameterContext.getParameter().getType();

        if (parameterType != String.class && parameterType != List.class && parameterType != Set.class) {
            return false;
        }

        if ((parameterType == List.class || parameterType == Set.class) &&
                !isStringCollection(parameterContext.getParameter().getParameterizedType())) {
            return false;
        }

        if (parameterContext.isAnnotated(RandomString.class)) {
            return true;
        }
        var testMethod = extensionContext.getTestMethod();
        return testMethod.isPresent() && testMethod.get().isAnnotationPresent(RandomString.class);
    }

    /**
     * Resolves the parameter by generating a random string, list, or set based on annotation configuration.
     * <p>
     * Priority: Parameter-level > Method-level > Default (10, alphanumeric, size 0).
     * <p>
     * Note: The type has already been validated by {@link #supportsParameter}, so the final branch in
     * {@link #generateValue} is a defensive guard that should not be reachable in normal usage.
     *
     * @param parameterContext information about the parameter to be resolved
     * @param extensionContext the current extension context
     * @return a randomly generated string, list, or set matching the annotation specifications
     * @throws IllegalArgumentException if the annotation specifies invalid parameters
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var parameterType = parameterContext.getParameter().getType();
        var parameterAnnotation = parameterContext.findAnnotation(RandomString.class);

        if (parameterAnnotation.isPresent()) {
            var annotation = parameterAnnotation.get();
            return generateValue(parameterType, annotation.size(), annotation.length(), annotation.characters());
        }

        var testMethod = extensionContext.getTestMethod();
        if (testMethod.isPresent()) {
            var methodAnnotation = testMethod.get().getAnnotation(RandomString.class);
            if (methodAnnotation != null) {
                return generateValue(parameterType, methodAnnotation.size(), methodAnnotation.length(),
                        methodAnnotation.characters());
            }
        }

        return generateValue(parameterType, 0, 10, TestingUtils.ALPHANUMERIC_CHARACTERS);
    }

    // Generates a list of random strings.
    private List<String> generateRandomStringList(int size, int length, String characters) {
        var list = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            list.add(TestingUtils.generateRandomString(length, characters));
        }
        return list;
    }

    // Generates a set of unique random strings.
    private Set<String> generateRandomStringSet(int size, int length, String characters) {
        if (characters.length() <= 62 && length <= 18) {
            long maxPossibleStrings = (long) Math.pow(characters.length(), length);
            if (size > maxPossibleStrings) {
                throw new IllegalArgumentException(
                        "Cannot generate %d unique strings of length %d from %d characters. "
                                .formatted(size, length, characters.length()) +
                                "Maximum possible unique strings: %d".formatted(maxPossibleStrings));
            }
        }

        // When size is large, maxAttempts can exceed Integer.MAX_VALUE causing the guard to
        // never trigger. Both variables are now long to ensure correct comparison.
        var set = new HashSet<String>(size * 4 / 3 + 1);
        long maxAttempts = (long) size * 100;
        long attempts = 0;

        while (set.size() < size) {
            if (attempts++ > maxAttempts) {
                throw new IllegalStateException(
                        "Failed to generate %d unique strings after %d attempts. "
                                .formatted(size, maxAttempts) +
                                "Consider using longer strings or more characters.");
            }
            set.add(TestingUtils.generateRandomString(length, characters));
        }
        return set;
    }

    // Generates the appropriate value based on the parameter type.
    // Note: this method is only called after supportsParameter() has already validated the type,
    // so the final throw branch is a defensive guard and should not be reachable in normal usage.
    @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
    private Object generateValue(Class<?> parameterType, int size, int length, String characters) {
        if (parameterType == String.class) {
            return TestingUtils.generateRandomString(length, characters);
        } else if (parameterType == List.class) {
            return generateRandomStringList(size, length, characters);
        } else if (parameterType == Set.class) {
            return generateRandomStringSet(size, length, characters);
        }
        throw new IllegalArgumentException(
                "Unsupported parameter type: %s. Supported types are String, List, and Set."
                        .formatted(parameterType));
    }

    // Checks if a parameterized type is a collection of strings.
    private boolean isStringCollection(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            var typeArguments = parameterizedType.getActualTypeArguments();
            return typeArguments.length == 1 && typeArguments[0] == String.class;
        }
        return false;
    }
}