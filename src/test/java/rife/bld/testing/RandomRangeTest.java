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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(RandomRangeResolver.class)
@SuppressWarnings("PMD.SystemPrintln")
class RandomRangeTest {

    @RepeatedTest(3)
    void defaultRange(@RandomRange int randomNum) {
        assertTrue(randomNum >= 0 && randomNum <= 100,
                "Random number should be between 0 and 100, but was: " + randomNum);
        System.out.println("Generated random number (default range): " + randomNum);
    }

    @RepeatedTest(3)
    void multipleRandomParameters(@RandomRange(min = 1, max = 5) int first,
                                  @RandomRange(min = 10, max = 20) int second) {
        assertTrue(first >= 1 && first <= 5);
        assertTrue(second >= 10 && second <= 20);
        System.out.println("First: " + first + ", Second: " + second);
    }

    @RepeatedTest(3)
    void negativeRange(@RandomRange(min = -50, max = -10) int randomNum) {
        assertTrue(randomNum >= -50 && randomNum <= -10,
                "Random number should be between -50 and -10, but was: " + randomNum);
        System.out.println("Generated random number (negative range): " + randomNum);
    }

    @RepeatedTest(3)
    void randomNumber(@RandomRange(min = 1, max = 10) int randomNum) {
        assertTrue(randomNum >= 1 && randomNum <= 10,
                "Random number should be between 1 and 10, but was: " + randomNum);
        System.out.println("Generated random number: " + randomNum);
    }
}

