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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlankSourceTest {

    @Nested
    @DisplayName("BlankSource tests")
    class BlankSourceTests {

        @ParameterizedTest(name = "case {index}: {0}")
        @DisplayName("String.isBlank() should return true for all blank inputs")
        @BlankSource
        void isBlankShouldReturnTrueForBlankSource(String input) {
            assertTrue(input.isBlank(),
                    () -> "Failed for input: '" + input + "'");
        }
    }

    @Nested
    @DisplayName("Non-blank cases")
    class NonBlankTests {

        @ParameterizedTest(name = "\"{0}\" should not be blank")
        @DisplayName("isBlank() returns false for real content")
        @ValueSource(strings = {"a", " test ", "x\ny", "0"})
        void isBlankShouldReturnFalse(String input) {
            assertFalse(input.isBlank());
        }
    }

    @Nested
    @DisplayName("Null handling")
    class NullTests {

        @ParameterizedTest
        @DisplayName("Combine @NullSource with @BlankSource")
        @NullSource
        @BlankSource
        void shouldHandleNullAndBlank(String input) {
            assertTrue(input == null || input.isBlank());
        }
    }
}


