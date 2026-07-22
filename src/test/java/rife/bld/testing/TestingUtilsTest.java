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
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestingUtilsTest {

    @Nested
    @DisplayName("Character Set Constants")
    class CharacterSetConstants {

        @Test
        @DisplayName("should verify alphanumeric character set")
        void verifyAlphanumericCharacterSet() {
            for (var c : TestingUtils.ALPHANUMERIC_CHARACTERS.toCharArray()) {
                assertTrue(Character.isLetterOrDigit(c), "Invalid character: " + c);
            }
        }

        @Test
        @DisplayName("should verify hexadecimal character set")
        void verifyHexadecimalCharacterSet() {
            for (var c : TestingUtils.HEXADECIMAL_CHARACTERS.toCharArray()) {
                assertTrue(Character.isDigit(c) || (c >= 'A' && c <= 'F'), "Invalid character: " + c);
            }
        }

        @Test
        @DisplayName("should verify lowercase character set")
        void verifyLowercaseCharacterSet() {
            for (var c : TestingUtils.LOWERCASE_CHARACTERS.toCharArray()) {
                assertTrue(Character.isLowerCase(c), "Invalid character: " + c);
            }
        }

        @Test
        @DisplayName("should verify numeric character set")
        void verifyNumericCharacterSet() {
            for (var c : TestingUtils.NUMERIC_CHARACTERS.toCharArray()) {
                assertTrue(Character.isDigit(c), "Invalid character: " + c);
            }
        }

        @Test
        @DisplayName("should verify uppercase character set")
        void verifyUppercaseCharacterSet() {
            for (var c : TestingUtils.UPPERCASE_CHARACTERS.toCharArray()) {
                assertTrue(Character.isUpperCase(c), "Invalid character: " + c);
            }
        }

        @Test
        @DisplayName("should verify URL-safe character set")
        void verifyUrlSafeCharacterSet() {
            for (var c : TestingUtils.URL_SAFE_CHARACTERS.toCharArray()) {
                assertTrue(Character.isLetterOrDigit(c) || c == '-' || c == '_',
                        "Invalid character: " + c);
            }
        }
    }

    @Nested
    @DisplayName("Generate Random Range Tests")
    class GenerateRandomRangeTests {

        @RepeatedTest(6)
        void generateRandomInt() {
            var randomInt = TestingUtils.generateRandomInt(0, 100);
            assertTrue(randomInt >= 0 && randomInt <= 100,
                    "Result should be between 0 and 100, but was: " + randomInt);
        }

        @Test
        void minGreaterThanMax() {
            var exception = assertThrows(IllegalArgumentException.class, () ->
                    TestingUtils.generateRandomInt(20, 10));
            var expectedMessage = "The minimum value (20) cannot be greater than maximum value (10)";
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Predefined Character Sets")
    class PredefinedCharacterSets {

        @RepeatedTest(3)
        @DisplayName("should generate string with alphanumeric characters")
        void generateWithAlphanumericCharacters() {
            var result = TestingUtils.generateRandomString(10, TestingUtils.ALPHANUMERIC_CHARACTERS);

            assertTrue(result.matches("[A-Za-z0-9]+"), "Result: " + result);
        }

        @RepeatedTest(3)
        @DisplayName("should generate string with hexadecimal characters")
        void generateWithHexadecimalCharacters() {
            var result = TestingUtils.generateRandomString(10, TestingUtils.HEXADECIMAL_CHARACTERS);

            assertTrue(result.matches("[0-9A-F]+"), "Result: " + result);
        }

        @RepeatedTest(3)
        @DisplayName("should generate string with lowercase characters")
        void generateWithLowercaseCharacters() {
            var result = TestingUtils.generateRandomString(10, TestingUtils.LOWERCASE_CHARACTERS);

            assertTrue(result.matches("[a-z]+"), "Result: " + result);
        }

        @RepeatedTest(3)
        @DisplayName("should generate string with numeric characters")
        void generateWithNumericCharacters() {
            var result = TestingUtils.generateRandomString(10, TestingUtils.NUMERIC_CHARACTERS);

            assertTrue(result.matches("[0-9]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should generate string with uppercase characters")
        void generateWithUppercaseCharacters() {
            var result = TestingUtils.generateRandomString(10, TestingUtils.UPPERCASE_CHARACTERS);

            assertTrue(result.matches("[A-Z]+"), "Result: " + result);
        }

        @Test
        @DisplayName("should generate string with URL-safe characters")
        void generateWithUrlSafeCharacters() {
            var result = TestingUtils.generateRandomString(10, TestingUtils.URL_SAFE_CHARACTERS);

            assertTrue(result.matches("[A-Za-z0-9\\-_]+"), "Result: " + result);
        }
    }
}
