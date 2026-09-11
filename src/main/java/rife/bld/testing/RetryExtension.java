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
import java.util.stream.IntStream;
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
 * <h4>Limitations:</h4>
 * <ul>
 *     <li>{@code @TestTemplate} requires all possible invocation contexts to be provided
 *     upfront, so up to {@link RetryTest#value()} invocation contexts are always created,
 *     even once the outcome has already been decided by an earlier attempt.</li>
 *     <li>{@code @BeforeEach}/{@code @AfterEach} callbacks and test instance construction
 *     run for every provided invocation context, including ones skipped after the outcome
 *     is finalized. This is a cost of the {@code @TestTemplate} SPI and is not avoidable
 *     without a custom provider that dynamically shrinks the context count, which is not
 *     supported.</li>
 *     <li>Retry state is shared across sibling invocations via the parent
 *     {@link ExtensionContext.Store}, keyed per test method; it assumes invocation contexts
 *     for the same method run sequentially, not concurrently.</li>
 * </ul>
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

        var method = methodOpt.get();
        var retryTest = method.getAnnotation(RetryTest.class);
        if (retryTest == null) {
            invocation.proceed();
            return;
        }

        int maxExecutions = retryTest.value();
        if (maxExecutions < 1) {
            throw new ExtensionConfigurationException("@RetryTest value must be >= 1, but was " + maxExecutions);
        }

        var state = retryStateFor(extensionContext, method);

        if (state.finished) {
            // Outcome already decided by an earlier invocation; don't re-run the method.
            return;
        }
        state.attempt++;

        try {
            invocation.proceed();
            state.finished = true;
        } catch (Throwable t) {
            var isLast = state.attempt >= maxExecutions;
            var retryable = shouldRetry(retryTest, t);
            printError(t, state.attempt);

            if (!retryable || isLast) {
                state.finished = true;
                throw t;
            }

            int delaySeconds = retryTest.delay();
            if (delaySeconds > 0) {
                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    t.addSuppressed(e);
                    state.finished = true;
                    throw t;
                }
            }
            // swallow: retryable, attempts remain — this invocation reports as passed,
            // and the next invocation context is given to JUnit
        }
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
        var maxExecutions = Math.max(retry.value(), 1);

        return IntStream.rangeClosed(1, maxExecutions)
                .mapToObj(i -> new TestTemplateInvocationContext() {
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

    private RetryState retryStateFor(ExtensionContext extensionContext, Method method) {
        var store = storeFor(extensionContext, method);
        var state = store.get(RetryState.class, RetryState.class);
        if (state == null) {
            state = new RetryState();
            store.put(RetryState.class, state);
        }
        return state;
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

    private ExtensionContext.Store storeFor(ExtensionContext context, Method method) {
        var templateContext = context.getParent().orElse(context);
        return templateContext.getStore(ExtensionContext.Namespace.create(RetryExtension.class, method));
    }

    private static final class RetryState {

        int attempt;
        boolean finished;
    }
}