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
import org.opentest4j.TestAbortedException;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JUnit extension that implements the {@link CouldFail} annotation.
 *
 * <p>This extension is automatically applied when {@link CouldFail} is present,
 * via {@code @ExtendWith(CouldFailExtension.class)} declared on that annotation.</p>
 *
 * <h2>Execution Flow</h2>
 * <ol>
 *   <li>Test executes normally</li>
 *   <li>If test passes, extension does nothing</li>
 *   <li>If test fails, {@code handleTestExecutionException} is called:
 *     <ol>
 *       <li>Extension checks for {@code @CouldFail} on method, then class</li>
 *       <li>If annotation not found, the test fails normally</li>
 *       <li>If {@code withExceptions} is empty, accept any exception</li>
 *       <li>If {@code withExceptions} is specified, check if thrown exception matches</li>
 *       <li>If the exception is accepted: throw {@code TestAbortedException}</li>
 *       <li>If the exception is not accepted: re-throw the original exception</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * <h2>Exception Matching</h2>
 * <p>An exception is considered "accepted" if:</p>
 * <ul>
 *   <li>{@code withExceptions} is empty (accepts any exception), OR</li>
 *   <li>The thrown exception, or any exception in its cause chain, is an instance
 *       of any class in {@code withExceptions}</li>
 * </ul>
 *
 * <h2>Logging</h2>
 * <p>When a failure is accepted, an INFO-level log message is generated:</p>
 * <pre>
 * Test failure accepted by @CouldFail (Original exception: IOException: Connection timeout)
 * </pre>
 * <p>The full stack trace of the original failure is logged at FINE level for debugging.</p>
 *
 * @see CouldFail
 * @see TestExecutionExceptionHandler
 * @since 1.0
 */
public class CouldFailExtension implements TestExecutionExceptionHandler {

    private static final String ABORT_MESSAGE = "Test marked @CouldFail — accepted as non-fatal";
    private static final String LOG_PREFIX = "Test failure accepted by @CouldFail";
    private static final Logger logger = Logger.getLogger(CouldFailExtension.class.getName());

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {

        var annotation = findCouldFailAnnotation(context);

        if (annotation.isEmpty()) {
            // No @CouldFail annotation, propagate exception normally
            throw throwable;
        }

        var couldFail = annotation.get();

        if (shouldAcceptFailure(couldFail, throwable)) {
            var message = throwable.getLocalizedMessage() != null ? throwable.getLocalizedMessage() : "(no message)";
            logger.log(Level.INFO,
                    "{0}: {1} [{2}]",
                    new Object[]{LOG_PREFIX, message, throwable.getClass().getSimpleName()}
            );
            logger.log(Level.FINE, "Original failure stack trace", throwable);
            throw new TestAbortedException(ABORT_MESSAGE, throwable);
        } else {
            // Exception type not accepted — fail normally
            throw throwable;
        }
    }

    private Optional<CouldFail> findCouldFailAnnotation(ExtensionContext context) {
        // Check method first, then class
        return context.getTestMethod()
                .flatMap(method -> Optional.ofNullable(method.getAnnotation(CouldFail.class)))
                .or(() -> context.getTestClass()
                        .flatMap(clazz -> Optional.ofNullable(clazz.getAnnotation(CouldFail.class))));
    }

    /**
     * Returns {@code true} if {@code throwable} or any exception in its cause chain
     * is an instance of {@code type}.
     *
     * <p>This allows {@code withExceptions = IOException.class} to match even when the
     * test throws a wrapping {@code RuntimeException} whose cause is an {@code IOException}.</p>
     *
     * @param type      the exception type to look for
     * @param throwable the throwable to inspect
     * @return {@code true} if any exception in the cause chain is an instance of {@code type}
     */
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

    private boolean shouldAcceptFailure(CouldFail couldFail, Throwable throwable) {
        var withExceptions = couldFail.withExceptions();

        // If no exceptions specified, accept any exception
        if (withExceptions.length == 0) {
            return true;
        }

        // Check if the thrown exception, or any cause in its chain, matches a specified type
        for (var exceptionType : withExceptions) {
            if (matchesCauseChain(exceptionType, throwable)) {
                return true;
            }
        }

        return false;
    }
}
