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

import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * JUnit extension that retries a failing {@link RetryTest} method.
 * <p>
 * Implements {@link TestTemplateInvocationContextProvider} and {@link InvocationInterceptor}
 * to integrate with {@code @TestTemplate}. The test is retried up to {@link RetryTest#value()}
 * times with an optional {@link RetryTest#delay()} in seconds.
 * <p>
 * If {@link RetryTest#withExceptions()} is specified, only matching exception types
 * (including causes in the cause chain) will trigger a retry.
 * <p>
 * If a retry succeeds, the test passes. If all retries fail, the last exception is thrown.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @author <a href="https://glaforge.dev/posts/2024/09/01/a-retryable-junit-5-extension/">Guillaume Laforge</a>
 * @see RetryTest
 * @since 1.0
 */
public class RetryExtension implements TestTemplateInvocationContextProvider, InvocationInterceptor {

    @Override
    @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AvoidCatchingGenericException"})
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext)
            throws Throwable {
        var methodOpt = extensionContext.getTestMethod();
        if (methodOpt.isEmpty()) {
            invocation.proceed();
            return;
        }

        var retryTest = methodOpt.get().getAnnotation(RetryTest.class);
        if (retryTest == null) {
            invocation.proceed();
            return;
        }

        int maxExecutions = retryTest.value();
        if (maxExecutions < 1) {
            throw new ExtensionConfigurationException(
                    "@RetryTest value must be >= 1, but was " + maxExecutions);
        }

        int delaySeconds = retryTest.delay();
        Throwable lastThrown = null;

        for (var i = 1; i <= maxExecutions; i++) {
            try {
                invocation.proceed();
                return;
            } catch (Throwable t) {
                lastThrown = t;

                // If exception is not accepted for retry, fail fast
                if (!shouldRetry(retryTest, t)) {
                    throw t;
                }

                if (i < maxExecutions) {
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
                }
            }
        }

        printError(lastThrown, maxExecutions);
        throw lastThrown;
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod()
                .filter(m -> m.isAnnotationPresent(RetryTest.class))
                .isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        var method = context.getRequiredTestMethod();
        var retry = method.getAnnotation(RetryTest.class);

        return Stream.of(new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return retry.name().isEmpty() ? method.getName() : retry.name();
            }
        });
    }

    private String getMessageRecursively(Throwable e) {
        if (e == null) {
            return "Unknown error";
        }
        if (e.getLocalizedMessage() != null) {
            return e.getLocalizedMessage() + " [" + e.getClass().getName() + "]";
        }
        if (e.getCause() == null || e.getCause().equals(e)) {
            return "No message [" + e.getClass().getName() + "]";
        }
        return getMessageRecursively(e.getCause());
    }

    private boolean matchesCauseChain(Class<? extends Throwable> type, Throwable throwable) {
        var current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @SuppressWarnings("PMD.SystemPrintln")
    private void printError(Throwable e, int count) {
        var message = getMessageRecursively(e);
        System.err.printf("Retry #%d failed (%s thrown): %s%n", count, e.getClass().getName(), message);
    }

    private boolean shouldRetry(RetryTest retryTest, Throwable throwable) {
        var withExceptions = retryTest.withExceptions();
        if (withExceptions == null || withExceptions.length == 0) {
            return true;
        }
        for (var exceptionType : withExceptions) {
            if (exceptionType != null && matchesCauseChain(exceptionType, throwable)) {
                return true;
            }
        }
        return false;
    }
}