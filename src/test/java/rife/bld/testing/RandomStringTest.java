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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(RandomStringResolver.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RandomStringTest {

    @Nested
    @DisplayName("Parameter Injection Integration")
    class ParameterInjectionIntegration {

        @Test
        @DisplayName("should inject custom character set string")
        void injectCustomCharacterSetString(
                @RandomString(
                        length = 8,
                        characters = TestingUtils.UPPERCASE_CHARACTERS
                ) String randomStr) {
            assertNotNull(randomStr);
            assertEquals(8, randomStr.length());
            assertTrue(randomStr.matches("[A-Z]+"), "Result: " + randomStr);
        }

        @Test
        @DisplayName("should inject custom length string")
        void injectCustomLengthString(@RandomString(length = 15) String randomStr) {
            assertNotNull(randomStr);
            assertEquals(15, randomStr.length());
            assertTrue(randomStr.matches("[A-Za-z0-9]+"), "Result: " + randomStr);
        }

        @Test
        @DisplayName("should inject default random string")
        void injectDefaultRandomString(@RandomString String randomStr) {
            assertNotNull(randomStr);
            assertEquals(10, randomStr.length());
            assertTrue(randomStr.matches("[A-Za-z0-9]+"), "Result: " + randomStr);
        }

        @Test
        @DisplayName("should inject hexadecimal string")
        void injectHexadecimalString(
                @RandomString(
                        length = 16,
                        characters = TestingUtils.HEXADECIMAL_CHARACTERS
                ) String hexStr) {
            assertNotNull(hexStr);
            assertEquals(16, hexStr.length());
            assertTrue(hexStr.matches("[0-9A-F]+"), "Result: " + hexStr);
        }

        @Test
        @DisplayName("should inject multiple different random strings")
        void injectMultipleDifferentRandomStrings(
                @RandomString(
                        length = 6,
                        characters = TestingUtils.NUMERIC_CHARACTERS
                ) String numbersOnly,
                @RandomString(length = 12) String defaultStr) {

            assertNotNull(numbersOnly);
            assertEquals(6, numbersOnly.length());
            assertTrue(numbersOnly.matches("[0-9]+"), "Result: " + numbersOnly);

            assertNotNull(defaultStr);
            assertEquals(12, defaultStr.length());
            assertTrue(defaultStr.matches("[A-Za-z0-9]+"));

            assertNotEquals(numbersOnly, defaultStr);
        }

        @Test
        @DisplayName("should inject URL-safe string")
        void injectUrlSafeString(
                @RandomString(
                        length = 20,
                        characters = TestingUtils.URL_SAFE_CHARACTERS
                ) String urlSafeStr) {
            assertNotNull(urlSafeStr);
            assertEquals(20, urlSafeStr.length());
            assertTrue(urlSafeStr.matches("[A-Za-z0-9\\-_]+"), "Result: " + urlSafeStr);
        }
    }
}
