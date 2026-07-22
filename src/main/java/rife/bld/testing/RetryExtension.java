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
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.lang.reflect.InvocationTargetException;

/**
 * JUnit extension that handles retry logic for test methods annotated
 * with the {@link RetryTest @RetryTest} annotation.
 * <p>
 * This extension will execute a test method multiple times on failure
 * based on the parameters specified in the {@link RetryTest @RetryTest}
 * annotation. It also supports adding a delay between retries to allow
 * external systems or resources to stabilize.
 * <p>
 * The extension uses {@link TestExecutionExceptionHandler} to intercept
 * and handle exceptions thrown during test execution. If the test succeeds
 * in any of the retry attempts, it is marked as passed. Otherwise, the
 * last exception thrown is re-thrown to indicate failure.
 * <p>
 * Features:
 * <ul>
 * <li>Retries a test upon failure, up to the specified maximum attempts.</li>
 * <li>Optionally introduces a delay between retry attempts.</li>
 * <li>Reports the failure count and exception details for each retry on the console.</li>
 * </ul>
 * <p>
 * Exceptions encountered during the retry process are carefully handled:
 * <ul>
 * <li>If a retry is interrupted during the delay, the thread is re-interrupted,
 * and the {@link InterruptedException} is added as a suppressed exception.</li>
 * <li>The original or last exception encountered is re-thrown when retries are exhausted.</li>
 * </ul>
 * <p>
 * This extension operates on individual test methods and requires them
 * to be annotated with {@link RetryTest @RetryTest} to activate the retry logic.
 * <p>
 * <strong>Note:</strong> Runtime exceptions during retries, such as {@link InvocationTargetException},
 * are unwrapped to reveal the underlying cause.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @author <a href="https://glaforge.dev/posts/2024/09/01/a-retryable-junit-5-extension/">Guillaume Laforge</a>
 * @since 1.0
 */
public class RetryExtension implements TestExecutionExceptionHandler {

    /**
     * Handles test execution exceptions, allowing retry mechanisms for a test method
     * annotated with {@link RetryTest @RetryTest}. This method retries the test execution
     * up to a specified number of attempts and introduces optional delays between retries.
     * <p>
     * If the test is not annotated with {@link RetryTest @RetryTest}, the exception is
     * rethrown immediately. If all retry attempts fail, the last exception is thrown.
     *
     * @param extensionContext the context in which the current test is executed,
     *                         providing information about the test method and instance
     * @param throwable        the exception thrown during the initial execution of the test
     * @throws Throwable if the test exhausts all retry attempts or is not eligible for retry
     */
    @Override
    @SuppressWarnings("PMD.DoNotUseThreads")
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        var testMethodOpt = extensionContext.getTestMethod();
        if (testMethodOpt.isEmpty()) {
            throw throwable;
        }
        var method = testMethodOpt.get();
        var retryTest = method.getAnnotation(RetryTest.class);

        if (retryTest == null) {
            throw throwable;
        }

        int maxExecutions = retryTest.value();
        int delaySeconds = retryTest.delay();
        var lastThrown = throwable;

        for (var i = 1; i < maxExecutions; i++) {
            printError(lastThrown, i);

            if (delaySeconds > 0) {
                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lastThrown.addSuppressed(e);
                    throw lastThrown;
                }
            }

            try {
                method.invoke(extensionContext.getRequiredTestInstance());
                // Succeeded, so return and mark the test as passed.
                return;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                lastThrown = e;
            }

        }

        printError(lastThrown, maxExecutions);
        throw lastThrown;
    }

    private String getMessageRecursively(Throwable e) {
        if (e == null) {
            return "Unknown error";
        }
        if (e.getLocalizedMessage() != null) {
            return e.getLocalizedMessage() + " [" + e.getClass().getName() + "]";
        }
        return getMessageRecursively(e.getCause());
    }

    @SuppressWarnings("PMD.SystemPrintln")
    private void printError(Throwable e, int count) {
        String message = getMessageRecursively(e);
        System.err.printf("Retry #%d failed (%s thrown): %s%n", count, e.getClass().getName(), message);
    }
}
