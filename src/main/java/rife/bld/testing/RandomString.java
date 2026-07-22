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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation for configuring random string generation in test method parameters.
 *
 * <p>Apply this annotation to {@code String} parameters in test methods to automatically
 * inject randomly generated strings. The extension will resolve these parameters during
 * test execution.
 *
 * <p>This annotation can also be applied at the method level to configure default settings
 * for all String parameters in that method.
 *
 * <h4>Usage examples:</h4>
 *
 * <blockquote><pre>
 * &#64;ExtendWith(RandomStringResolver.class)
 * public class MyTest {
 *     // Default: 10 alphanumeric characters
 *     &#64;Test
 *     void test(&#64;RandomString String str) { ... }
 *
 *     // Custom length: 15 characters
 *     &#64;Test
 *     void test(&#64;RandomString(length = 15) String str) { ... }
 *
 *     // Custom character set: only uppercase letters
 *     &#64;Test
 *     void test(&#64;RandomString(characters = TestingUtils.UPPERCASE_CHARACTERS) String str) { ... }
 *
 *     // Both custom length and characters: 8-character hex string
 *     void test(&#64;RandomString(length = 8, characters = TestingUtils.HEXADECIMAL_CHARACTERS) String hexStr) { ... }
 *
 *     // Multiple parameters
 *     &#64;Test
 *     void test(&#64;RandomString(characters = "ABC123") String str1, &#64;RandomString(length = 5) String str2) { ... }
 *
 *     // Method-level annotation for single parameter
 *     &#64;Test
 *     &#64;RandomString(length = 5)
 *     void test(String random) { ... }
 *
 *     // Method-level annotation applies to all String parameters
 *     &#64;Test
 *     &#64;RandomString(length = 8, characters = TestingUtils.URL_SAFE_CHARACTERS)
 *     void test(String url1, String url2) { ... }
 *
 *     // Parameter-level annotation overrides method-level
 *     &#64;Test
 *     &#64;RandomString(length = 5)
 *     void test(String defaultRandom, &#64;RandomString(length = 3) String shortRandom) { ... }
 *
 *     // Field injection
 *     &#64;RandomString
 *     private String myRandomString;
 *
 *     &#64;Test
 *     void test() {
 *         // myRandomString is initialized before test
 *     }
 *
 *     // List of random strings
 *     &#64;Test
 *     void test(&#64;RandomString(size = 5) List&lt;String&gt; randomList) { ... }
 *
 *     // Set of random strings with custom length
 *     &#64;Test
 *     void test(&#64;RandomString(size = 10, length = 8) Set&lt;String&gt; randomSet) { ... }
 *
 *     // List with custom character set
 *     &#64;Test
 *     void test(&#64;RandomString(size = 3, characters = TestingUtils.HEXADECIMAL_CHARACTERS) List&lt;String&gt; hexList) { ... }
 *
 *     // Field injection for List
 *     &#64;RandomString(size = 5)
 *     private List&lt;String&gt; randomStrings;
 *
 *     // Field injection for Set
 *     &#64;RandomString(size = 10, length = 15)
 *     private Set&lt;String&gt; randomStringSet;
 * }</pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see RandomStringResolver
 * @see TestingUtils#ALPHANUMERIC_CHARACTERS
 * @see TestingUtils#HEXADECIMAL_CHARACTERS
 * @see TestingUtils#LOWERCASE_CHARACTERS
 * @see TestingUtils#NUMERIC_CHARACTERS
 * @see TestingUtils#UPPERCASE_CHARACTERS
 * @see TestingUtils#URL_SAFE_CHARACTERS
 * @since 1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RandomStringResolver.class)
public @interface RandomString {

    /**
     * The alphanumeric character set to use for random string generation.
     *
     * @return the character set string
     * @throws IllegalArgumentException if the character set is {@code null} or empty during resolution
     */
    String characters() default TestingUtils.ALPHANUMERIC_CHARACTERS;

    /**
     * The length of the generated random string.
     *
     * @return the desired length
     * @throws IllegalArgumentException if length is 0 or negative during resolution
     */
    int length() default 10;

    /**
     * The number of random strings to generate for List or Set parameters.
     * <p>
     * When {@code size} is greater than 0, this annotation can be applied to:
     * <ul>
     *   <li>{@code List<String>} parameters - generates a list of random strings</li>
     *   <li>{@code Set<String>} parameters - generates a set of unique random strings</li>
     * </ul>
     * <p>
     * When {@code size} is 0 (default), this annotation applies to single {@code String} parameters.
     *
     * @return the number of strings to generate, or 0 for single string generation
     * @throws IllegalArgumentException if size is negative during resolution
     */
    int size() default 0;
}