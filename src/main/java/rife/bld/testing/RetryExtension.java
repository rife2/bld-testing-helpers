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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JUnit 5 extension that implements retry logic for {@link RetryTest}.
 * <p>
 * Implements both {@link TestTemplateInvocationContextProvider} and {@link InvocationInterceptor}
 * to keep the implementation in a single extension.
 * <p>
 * <b>Bug fix - swallowed invocations:</b> Earlier versions swallowed a retryable exception
 * and returned normally, causing JUnit to count the invocation as {@code successful}.
 * That led to two visible bugs:
 * <ol>
 *   <li>IDE rerun of a single invocation:
 *       {@code --select-unique-id='.../invocation:#1'} on an always-failing test reported
 *       {@code 1 successful} because attempt #1 was swallowed.</li>
 *   <li>Count inflation: {@code @RetryTest(3)} passing first time contributed 3 greens,
 *       and always-failing contributed 2 passes + 1 failure.</li>
 * </ol>
 * The fix is to throw {@link TestAbortedException} for retryable intermediate failures
 * instead of swallowing. Aborted invocations are not counted as success.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see RetryTest
 * @since 1.0
 */
public class RetryExtension implements TestTemplateInvocationContextProvider, InvocationInterceptor {

    private final Map<Method, Optional<RetryTest>> retryTestCache = new ConcurrentHashMap<>();

    /**
     * Intercepts each test-template method invocation to apply retry logic.
     */
    @Override
    @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AvoidCatchingGenericException", "PMD.PreserveStackTrace"})
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_HAS_CHECKED")
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
        var retryTestOpt = findRetryTest(method);
        if (retryTestOpt.isEmpty()) {
            invocation.proceed();
            return;
        }
        var retryTest = retryTestOpt.get();

        validate(method, retryTest);

        var maxExecutions = retryTest.value();
        var state = retryStateFor(extensionContext, method);

        if (state.finished.get()) {
            invocation.skip();
            return;
        }
        var currentAttempt = state.attempt.incrementAndGet();

        try {
            invocation.proceed();
            state.finished.set(true);
        } catch (Throwable t) {
            var isLast = currentAttempt >= maxExecutions;
            var retryable = shouldRetry(retryTest, t);
            reportError(extensionContext, t, currentAttempt);

            if (!retryable || isLast) {
                state.finished.set(true);
                throw t;
            }

            int delaySeconds = retryTest.delay();
            if (delaySeconds > 0) {
                try {
                    // Required to implement the delay. SAME_THREAD execution mode ensures
                    // this runs on the test thread itself, so blocking is expected.
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    state.finished.set(true);
                    // Rethrow the interruption directly rather than converting it
                    // to a suppressed exception; the delay itself was interrupted,
                    // and that's the real reason the retry cannot proceed.
                    throw e;
                }
            }

            throw new TestAbortedException(
                    "Attempt " + currentAttempt + " of " + maxExecutions + " failed and will be retried", t);
        }
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod()
                .flatMap(this::findRetryTest)
                .isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        var method = context.getRequiredTestMethod();
        var retry = findRetryTest(method)
                .orElseThrow(() -> new ExtensionConfigurationException(
                        "No @RetryTest annotation found (directly or meta-present) on " + method));
        validate(method, retry);

        var maxExecutions = retry.value();
        return IntStream.rangeClosed(1, maxExecutions)
                .mapToObj(i -> new TestTemplateInvocationContext() {
                    @Override
                    public String getDisplayName(int invocationIndex) {
                        var baseName = retry.name().isEmpty() ? method.getName() : retry.name();
                        return baseName + " [" + invocationIndex + "/" + maxExecutions + "]";
                    }
                });
    }

    /**
     * Finds {@code @RetryTest} on the method, including as a meta-annotation.
     * Results are cached to avoid repeated reflection lookups.
     * Mock-friendly: does not rely on AnnotationUtils which NPEs when a mocked Method returns null arrays.
     */
    private Optional<RetryTest> findRetryTest(Method method) {
        return retryTestCache.computeIfAbsent(method, this::resolveRetryTest);
    }

    private String getMessageRecursively(Throwable e) {
        if (e == null) {
            return "Unknown error";
        }
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        var current = e;
        while (seen.add(current)) {
            if (current.getLocalizedMessage() != null) {
                return current.getLocalizedMessage() + " [" + current.getClass().getName() + "]";
            }
            var cause = current.getCause();
            if (cause == null || cause.equals(current)) {
                return "No message [" + current.getClass().getName() + "]";
            }
            current = cause;
        }
        // cycle detected
        return "No message [cycle detected: " + e.getClass().getName() + "]";
    }

    private boolean matchesCauseChain(Class<? extends Throwable> type, Throwable throwable) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        var current = throwable;
        while (current != null && seen.add(current)) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void reportError(ExtensionContext extensionContext, Throwable e, int count) {
        var message = getMessageRecursively(e);
        extensionContext.publishReportEntry(
                "Retry #" + count + " failed",
                "Exception: " + e.getClass().getName() + " - " + message);
    }

    private Optional<RetryTest> resolveRetryTest(Method method) {
        var direct = method.getAnnotation(RetryTest.class);
        if (direct != null) {
            return Optional.of(direct);
        }
        // getAnnotations() includes both declared and inherited annotations for methods
        for (Annotation ann : method.getAnnotations()) {
            var meta = ann.annotationType().getAnnotation(RetryTest.class);
            if (meta != null) {
                return Optional.of(meta);
            }
        }
        return Optional.empty();
    }

    private RetryState retryStateFor(ExtensionContext extensionContext, Method method) {
        return storeFor(extensionContext, method)
                .computeIfAbsent(RetryState.class, k -> new RetryState(), RetryState.class);
    }

    private boolean shouldRetry(RetryTest retryTest, Throwable throwable) {
        if (throwable instanceof TestAbortedException) {
            return false;
        }
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

    /**
     * Retrieves the store from the parent (method-level) extension context.
     * This intentional design ensures each test template method invocation shares
     * a single RetryState. The state is scoped to the method context and is
     * automatically cleaned up by JUnit when the test completes.
     */
    private ExtensionContext.Store storeFor(ExtensionContext context, Method method) {
        var templateContext = context.getParent()
                .orElseThrow(() -> new ExtensionConfigurationException(
                        "RetryExtension requires parent ExtensionContext for " + method));
        return templateContext.getStore(ExtensionContext.Namespace.create(RetryExtension.class, method));
    }

    private void validate(Method method, RetryTest retryTest) {
        if (retryTest.value() < 1) {
            throw new ExtensionConfigurationException(
                    "@RetryTest value must be >= 1, but was " + retryTest.value() + " on " + method);
        }
        if (retryTest.delay() < 0) {
            throw new ExtensionConfigurationException(
                    "@RetryTest delay must be >= 0, but was " + retryTest.delay() + " on " + method);
        }
        for (var ex : retryTest.withExceptions()) {
            if (ex == null) {
                throw new ExtensionConfigurationException(
                        "@RetryTest withExceptions must not contain null on " + method);
            }
        }
    }

    private static final class RetryState {

        final AtomicInteger attempt = new AtomicInteger(0);
        final AtomicBoolean finished = new AtomicBoolean(false);
    }
}