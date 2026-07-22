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

import java.security.SecureRandom;

/**
 * Provides static methods for generating random values and predefined character sets.
 *
 * <p>This class is thread-safe. The shared {@link SecureRandom} instance is safe for concurrent
 * use, making this utility suitable for parallel test frameworks (e.g. JUnit 5 parallel execution).
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestingUtils {

    /**
     * A string constant containing all uppercase letters, lowercase letters, and numeric digits.
     */
    public static final String ALPHANUMERIC_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    /**
     * A string constant containing all hexadecimal digits and letters.
     */
    public static final String HEXADECIMAL_CHARACTERS = "0123456789ABCDEF";
    /**
     * A string constant containing all lowercase letters.
     */
    public static final String LOWERCASE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    /**
     * A string constant containing all numeric digits.
     */
    public static final String NUMERIC_CHARACTERS = "0123456789";
    /**
     * A string constant containing all uppercase letters.
     */
    public static final String UPPERCASE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /**
     * A string constant representing a set of characters that are safe for use in URLs.
     * <p>
     * It includes uppercase and lowercase letters, digits, the symbols {@code -} and  {@code _}
     */
    public static final String URL_SAFE_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final SecureRandom secureRandom = new SecureRandom();

    private TestingUtils() {
    }

    /**
     * Generates a random integer within the specified range.
     *
     * <p>Note: {@code max} must not be {@link Integer#MAX_VALUE}, as {@code max + 1} would
     * overflow. Passing {@code Integer.MAX_VALUE} as {@code max} results in undefined behavior.
     *
     * @param min the minimum value (inclusive) of the random number
     * @param max the maximum value (inclusive) of the random number
     * @return a random integer between {@code min} and {@code max}, inclusive
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public static int generateRandomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(
                    "The minimum value (%d) cannot be greater than maximum value (%d)".formatted(min, max));
        }
        return secureRandom.nextInt(min, max + 1);
    }

    /**
     * Generates a random string with specified parameters.
     *
     * <p>Each character in the set is chosen with equal probability. Note that duplicate
     * characters in the {@code characters} argument will increase their relative selection
     * probability proportionally. The predefined character set constants in this class
     * contain no duplicates.
     *
     * @param length     the desired length of the generated string
     * @param characters the character set to use; duplicate characters increase their selection
     *                   probability
     * @return a randomly generated string of the specified length
     * @throws IllegalArgumentException if the length is non-positive or the character set is
     *                                  {@code null} or empty
     */
    public static String generateRandomString(int length, String characters) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        if (isEmpty(characters)) {
            throw new IllegalArgumentException("Characters cannot be null or empty");
        }

        var result = new StringBuilder(length);
        var charLen = characters.length();

        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(secureRandom.nextInt(charLen)));
        }
        return result.toString();
    }

    /**
     * Generates a random string with default parameters.
     *
     * <p>Equivalent to {@code generateRandomString(10, ALPHANUMERIC_CHARACTERS)}.
     *
     * @return a 10-character random alphanumeric string
     */
    public static String generateRandomString() {
        return generateRandomString(10, ALPHANUMERIC_CHARACTERS);
    }

    /**
     * Generates a random string with a specified length.
     *
     * <p>Equivalent to {@code generateRandomString(length, ALPHANUMERIC_CHARACTERS)}.
     *
     * @param length the desired length of the generated string
     * @return a random alphanumeric string of the specified length
     * @throws IllegalArgumentException if length is non-positive
     */
    public static String generateRandomString(int length) {
        return generateRandomString(length, ALPHANUMERIC_CHARACTERS);
    }

    /**
     * Determines if a string is null or empty
     *
     * @param s the string to check
     * @return {@code true} if the string is null or empty, {@code false} otherwise
     */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}