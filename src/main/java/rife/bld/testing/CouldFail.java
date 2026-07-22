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
 * JUnit annotation to mark a test as allowed to fail.
 *
 * <p>When a test annotated with {@code @CouldFail} fails, the failure is accepted
 * and the test is aborted (shown as skipped). If the test passes, it continues
 * to pass normally.</p>
 *
 * <p>This is similar to JUnit Pioneer's {@code @ExpectedToFail} but with inverted
 * logic: a passing test continues to pass (not fail), making it suitable for
 * acknowledging known issues without expecting them to be fixed.</p>
 *
 * <h2>Usage</h2>
 *
 * <h3>Accept Any Failure</h3>
 * <pre>{@code
 * @Test
 * @CouldFail
 * void testFlakeyOperation() {
 *     // Test that might fail - any exception will be accepted
 * }
 * }</pre>
 *
 * <h3>Accept Specific Exceptions</h3>
 * <pre>{@code
 * @Test
 * @CouldFail(withExceptions = UnsupportedOperationException.class)
 * void testUnimplementedFeature() {
 *     // Only UnsupportedOperationException will be accepted
 *     // Other exceptions will fail the test normally
 * }
 * }</pre>
 *
 * <h3>Accept Multiple Exception Types</h3>
 * <pre>{@code
 * @Test
 * @CouldFail(withExceptions = {IOException.class, TimeoutException.class})
 * void testNetworkOperation() {
 *     // IOException or TimeoutException will be accepted
 *     // Other exceptions will fail the test normally
 * }
 * }</pre>
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>Known bugs or issues that haven't been fixed yet</li>
 *   <li>Intermittent failures in CI/CD pipelines</li>
 *   <li>Platform-specific issues</li>
 *   <li>External service dependencies that may be unavailable</li>
 *   <li>Unimplemented features (stub code throwing UnsupportedOperationException)</li>
 * </ul>
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>If the test passes: continues to pass (no special handling)</li>
 *   <li>If the test fails with accepted exception: test is aborted (shown as skipped)</li>
 *   <li>If the test fails with non-accepted exception: test fails normally</li>
 * </ul>
 *
 * @see CouldFailExtension
 * @since 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CouldFailExtension.class)
public @interface CouldFail {

    /**
     * Specific exception types that will be accepted as failures.
     *
     * <p>If empty (default), any exception will be accepted. If specified,
     * only these exception types (and their subclasses) will be accepted.
     * Any other exception will cause the test to fail normally.</p>
     *
     * @return array of exception classes to accept
     */
    Class<? extends Throwable>[] withExceptions() default {};
}
