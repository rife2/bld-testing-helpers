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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.lang.annotation.*;
import java.util.stream.Stream;

/**
 * Parameterized test source that provides common blank string values.
 * <p>
 * This annotation is a composable replacement for using {@link ArgumentsSource}
 * directly. When applied to a {@code @ParameterizedTest} method, it supplies
 * a stream of blank strings covering the most common whitespace cases.
 * <p>
 * The following values are supplied:
 * <ul>
 *   <li>{@code ""} - empty string</li>
 *   <li>{@code " "} - single space</li>
 *   <li>{@code "   "} - multiple spaces</li>
 *   <li>{@code "\t"} - tab character</li>
 *   <li>{@code "\n"} - line feed / newline</li>
 *   <li>{@code "\r\n"} - carriage return + line feed (CRLF)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * @ParameterizedTest
 * @BlankSource
 * void shouldRejectBlank(String input) {
 *     assertTrue(input.isBlank());
 * }
 * }</pre>
 *
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.provider.ArgumentsProvider
 * @see String#isBlank()
 * @since 1.0
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(BlankSource.BlankArgumentsProvider.class)
public @interface BlankSource {

    /**
     * {@link ArgumentsProvider} implementation backing {@link BlankSource}.
     * <p>
     * Provides a fixed {@link Stream} of {@link Arguments} containing blank strings
     * for use with JUnit parameterized tests. This provider is not intended to be
     * used directly; it is wired via {@link ArgumentsSource} on the {@link BlankSource}
     * annotation.
     */
    class BlankArgumentsProvider implements ArgumentsProvider {

        /**
         * Provides the blank string arguments for the parameterized test.
         *
         * @param parameters the parameter declarations for the test method,
         *                   as provided by JUnit
         * @param context    the current extension context, as provided by JUnit
         * @return a stream of {@link Arguments} containing blank strings:
         * empty, space, multiple spaces, tab, newline and CRLF
         */
        @Override
        public Stream<? extends Arguments> provideArguments(
                ParameterDeclarations parameters,
                ExtensionContext context) {

            return Stream.of(
                    Arguments.of(""),      // empty
                    Arguments.of(" "),     // space
                    Arguments.of("   "),   // multiple spaces
                    Arguments.of("\t"),    // tab
                    Arguments.of("\n"),    // newline
                    Arguments.of("\r\n") // CRLF
            );
        }
    }
}