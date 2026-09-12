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
import java.util.*;
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
 * <b>Limitations:</b>
 * <ul>
 *   <li>All {@code value()} invocation contexts are created up front. A per-invocation
 *       {@link ExecutionCondition} disables an invocation once the outcome for the method
 *       is already decided, so unused invocations are reported as {@code disabled} rather
 *       than {@code passed} or {@code failed}.</li>
 *   <li>{@code @BeforeEach} and {@code @AfterEach} failures share the same attempt budget
 *       and retry policy as the test method itself. An {@code @AfterEach} failure that
 *       occurs after the invocation's outcome is already decided (the test passed, or an
 *       earlier phase's failure was ruled final) is reported as a plain failure rather
 *       than offered a retry, since no further invocation will run at that point.</li>
 * </ul>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see RetryTest
 * @since 1.0
 */
public class RetryExtension implements TestTemplateInvocationContextProvider, InvocationInterceptor {

    private final Map<Method, Optional<RetryTest>> retryTestCache = new ConcurrentHashMap<>();

    /**
     * Intercepts {@code @BeforeEach} so a setup failure is governed by the same attempt
     * budget and {@code withExceptions} policy as the test method itself, instead of
     * running {@code maxExecutions} times independently of the retry outcome.
     */
    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext,
                                          ExtensionContext extensionContext)
            throws Throwable {
        interceptLifecycleInvocation(invocation, extensionContext, false);
    }

    /**
     * Intercepts each test-template method invocation to apply retry logic.
     */
    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext)
            throws Throwable {
        interceptLifecycleInvocation(invocation, extensionContext, true);
    }

    /**
     * Intercepts {@code @AfterEach}. Unlike {@code @BeforeEach}, this can run in an
     * invocation where {@code state.finished} is already {@code true} — set moments
     * earlier, in this same invocation, by the test method passing or by its failure
     * being ruled final. {@link #interceptLifecycleInvocation} accounts for that: once
     * finished is true no further invocation will occur regardless of what happens here,
     * so an {@code @AfterEach} failure at that point is let through as a plain failure
     * rather than offered a retry it can never receive.
     */
    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> invocationContext,
                                         ExtensionContext extensionContext)
            throws Throwable {
        interceptLifecycleInvocation(invocation, extensionContext, false);
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
        var retry = findValidatedRetryTest(method)
                .orElseThrow(() -> new ExtensionConfigurationException(
                        "No @RetryTest annotation found (directly or meta-present) on " + method));

        var maxExecutions = retry.value();
        var gate = retryGate(method);
        return IntStream.rangeClosed(1, maxExecutions)
                .mapToObj(i -> new TestTemplateInvocationContext() {
                    @Override
                    public String getDisplayName(int invocationIndex) {
                        var baseName = retry.name().isEmpty() ? method.getName() : retry.name();
                        return baseName + " [" + invocationIndex + "/" + maxExecutions + "]";
                    }

                    @Override
                    public List<Extension> getAdditionalExtensions() {
                        return List.of(gate);
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

    /**
     * {@link #findRetryTest(Method)}, validated. Each of the three entry points
     * (provider, gate, lifecycle interceptor) calls this independently rather than
     * validating once and threading the result through, since the check itself is cheap
     * and this keeps each entry point self-contained.
     */
    private Optional<RetryTest> findValidatedRetryTest(Method method) {
        var retryTestOpt = findRetryTest(method);
        retryTestOpt.ifPresent(retryTest -> validate(method, retryTest));
        return retryTestOpt;
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

    /**
     * Shared retry handling for {@code @BeforeEach}, {@code @AfterEach}, and the
     * test-template method. The attempt counter is advanced once per invocation by the
     * {@link ExecutionCondition} returned from {@link #retryGate(Method)}, before any
     * phase runs; this method only reads it. Only {@code markSuccessOnCompletion} (the
     * test-template phase) marks the invocation as decided on success, so a passing
     * {@code @BeforeEach} does not itself end retries.
     * <p>
     * If {@code state.finished} is already {@code true} on entry — set by an earlier
     * phase of this same invocation — the retry decision has already been made and no
     * further invocation will run, so this phase's outcome is passed through as-is
     * instead of being offered a retry it can never receive. This only actually arises
     * for {@code @AfterEach}: {@link #retryGate(Method)} keeps {@code @BeforeEach} and
     * the test method from starting at all once finished is true.
     */
    @SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AvoidCatchingGenericException", "PMD.PreserveStackTrace"})
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS")
    private void interceptLifecycleInvocation(Invocation<Void> invocation,
                                              ExtensionContext extensionContext,
                                              boolean markSuccessOnCompletion)
            throws Throwable {
        var methodOpt = extensionContext.getTestMethod();
        if (methodOpt.isEmpty()) {
            invocation.proceed();
            return;
        }

        var method = methodOpt.get();
        var retryTestOpt = findValidatedRetryTest(method);
        if (retryTestOpt.isEmpty()) {
            invocation.proceed();
            return;
        }
        var retryTest = retryTestOpt.get();
        var state = retryStateFor(extensionContext, method);

        if (state.finished.get()) {
            invocation.proceed();
            return;
        }

        var maxExecutions = retryTest.value();
        var currentAttempt = state.attempt.get();

        try {
            invocation.proceed();
            if (markSuccessOnCompletion) {
                state.finished.set(true);
            }
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

    /**
     * Gates each invocation context so that once the outcome for the method is decided
     * (a passing invocation, or a non-retryable/last-attempt failure) the remaining,
     * already-created invocation contexts are reported as {@code disabled} instead of
     * running and being counted as {@code passed}. Also advances the shared attempt
     * counter exactly once per invocation, before {@code @BeforeEach} or the test method
     * runs, so {@link #interceptLifecycleInvocation} only needs to read it.
     */
    private ExecutionCondition retryGate(Method method) {
        return context -> {
            var retryTestOpt = findValidatedRetryTest(method);
            if (retryTestOpt.isEmpty()) {
                return ConditionEvaluationResult.enabled("No @RetryTest on " + method);
            }
            var state = retryStateFor(context, method);
            if (state.finished.get()) {
                return ConditionEvaluationResult.disabled(
                        "Retry outcome for " + method.getName() + " already decided");
            }
            var currentAttempt = state.attempt.incrementAndGet();
            return ConditionEvaluationResult.enabled("Attempt " + currentAttempt + " of " + method.getName());
        };
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