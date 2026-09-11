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

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation to mark a test method for retry on failure.
 * <p>
 * When a test fails, it will be retried up to the specified number of times.
 * Optionally, a wait period can be specified between retry attempts.
 * <p>
 * By default, any exception triggers a retry. If {@code withExceptions} is specified,
 * only matching exception types (or any exception in their cause chain) will trigger
 * a retry — other failures fail fast without retrying.
 * <p>
 * This annotation automatically includes the {@link RetryExtension}, so no additional
 * {@code @ExtendWith} annotation is required.
 *
 * <h4>Usage examples:</h4>
 *
 * <blockquote><pre>
 * &#64;RetryTest(3)
 * void unstableTest() {
 *     // Test code that might fail intermittently
 * }
 *
 * &#64;RetryTest(value = 5, delay = 2)
 * void testWithDelay() {
 *     // Test that waits 2 seconds between retry attempts
 * }
 *
 * &#64;RetryTest(value = 3, withExceptions = IOException.class)
 * void testRetryOnIoException() {
 *     // Only retries if IOException is thrown (or is a cause)
 * }
 *
 * &#64;RetryTest(withExceptions = {SocketTimeoutException.class, ConnectException.class})
 * void testRetryOnSpecificExceptions() {
 *     // Only retries on specific network exceptions
 * }</pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see RetryExtension
 * @since 1.0
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(RetryExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
public @interface RetryTest {

    /**
     * The number of seconds to wait between retry attempts.
     * <p>
     * If set to {@code 0} (default), no wait occurs between retries.
     * This can be useful for tests that interact with external systems
     * that may need time to recover or stabilize.
     *
     * @return the wait time in seconds between retry attempts
     */
    int delay() default 0;

    /**
     * Optional name for the test template. If not specified,
     * a default name will be generated.
     *
     * @return the display name for the retry test template
     */
    String name() default "";

    /**
     * The maximum number of retry attempts for a failing test.
     * <p>
     * The test will be executed at most {@code value()} times after the initial failure.
     *
     * @return the number of retry attempts. Must be greater than 0
     */
    int value() default 3;

    /**
     * Exception types that should trigger a retry.
     * <p>
     * If empty, any exception triggers a retry. If specified, only matching
     * exceptions (or their causes) will be retried.
     */
    Class<? extends Throwable>[] withExceptions() default {};
}
