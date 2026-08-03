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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JUnit extension for configuring console logging for test suites.
 * <p>
 * This extension sets up a console handler with a configurable logging level before each test method and restores
 * the original logger configuration after each test method completes. This provides maximum isolation between
 * individual test methods.
 * <p>
 * The logger is configured to output directly to the console without using parent handlers.
 *
 * <h3>Usage Examples:</h3>
 *
 * <blockquote><pre>
 * &#64;ExtendWith(LoggingExtension.class)
 * class MyTestClass {
 *     // Default configuration (uses LoggingExtension logger with Level.ALL)
 *     &#64;Test
 *     void myTest() { ... }
 *
 *     // Custom logger with default level
 *     &#64;RegisterExtension
 *     private static final LoggingExtension loggingExtension = new LoggingExtension("MyCustomLogger");
 *
 *     // Custom logger and level
 *     &#64;RegisterExtension
 *     private static final LoggingExtension loggingExtension = new LoggingExtension(
 *         MyClass.getLogger(),
 *         Level.INFO
 *     );
 *
 *     // Custom logger with test log handler
 *     private static final Logger logger = Logger.getLogger(MyClass.class.getName());
 *     private static final TestLogHandler testLogHandler = new TestLogHandler();
 *
 *     &#64;RegisterExtension
 *     private static final LoggingExtension loggingExtension = new LoggingExtension(
 *         logger,
 *         testLogHandler
 *     );
 *
 *     // Custom logger with existing handler and level override
 *     &#64;RegisterExtension
 *     private static final LoggingExtension loggingExtension = new LoggingExtension(
 *         MyClass.getLogger(),
 *         myExistingHandler,
 *         Level.WARNING
 *     );
 * }</pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @see AfterEachCallback
 * @see BeforeEachCallback
 * @see Level
 * @see Logger
 * @see TestLogHandler
 * @since 1.0
 */
@NullMarked
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
@SuppressWarnings("PMD.MoreThanOneLogger") // two loggers are intentional: default + injected
public class LoggingExtension implements BeforeEachCallback, AfterEachCallback {

    private static final String LOGGER_NAME_CANNOT_BE_NULL = "loggerName" + TestingUtils.CANNOT_BE_NULL;

    /**
     * Default logger instance used when no custom logger is specified.
     */
    private static final Logger defaultLogger = Logger.getLogger(LoggingExtension.class.getName());

    /**
     * Stores logger state per test invocation, keyed by class then unique test ID.
     * Using the full unique test ID (rather than just logger name) prevents races
     * when tests within the same class run concurrently.
     */
    private static final Map<Class<?>, Map<String, LoggerState>> testMethodConfigs = new ConcurrentHashMap<>();

    /**
     * The handler to use for logging output. If {@code null}, a {@link ConsoleHandler} will be created.
     * Stored directly (not copied) because the caller intentionally shares the same handler instance.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2") // intentional: caller owns the handler instance
    private final @Nullable Handler handler;

    /**
     * The logging level to set for both the logger and console handler.
     */
    private final Level level;

    /**
     * The logger instance to configure.
     */
    private final Logger logger;

    /**
     * Creates a LoggingExtension with the default logger and {@link Level#ALL}.
     * <p>
     * The default logger is named after
     * {@link LoggingExtension this class}.
     */
    public LoggingExtension() {
        this(defaultLogger, null, Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger and {@link Level#ALL}.
     *
     * @param logger the logger to configure for console output
     * @throws NullPointerException if logger is {@code null}
     */
    public LoggingExtension(Logger logger) {
        this(logger, null, Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger and logging level.
     *
     * @param logger the logger to configure for console output
     * @param level  the logging level to set for both logger and console handler
     * @throws NullPointerException if logger or level is {@code null}
     */
    public LoggingExtension(Logger logger, Level level) {
        this(logger, null, level);
    }

    /**
     * Creates a LoggingExtension with a custom logger and existing handler.
     * <p>
     * The handler's existing level will be preserved. If the handler has no level explicitly set
     * (i.e. {@link Handler#getLevel()} returns {@code null}), {@link Level#ALL} is used.
     *
     * @param logger  the logger to configure for output
     * @param handler the existing handler to use for logging output
     * @throws NullPointerException if logger or handler is {@code null}
     */
    public LoggingExtension(Logger logger, Handler handler) {
        this(Objects.requireNonNull(logger, "logger" + TestingUtils.CANNOT_BE_NULL),
                Objects.requireNonNull(handler, "handler" + TestingUtils.CANNOT_BE_NULL),
                handler.getLevel() != null ? handler.getLevel() : Level.ALL);
    }

    /**
     * Creates a LoggingExtension with a custom logger, existing handler, and logging level override.
     * <p>
     * The specified level will be applied to both the logger and the handler, overriding the handler's existing
     * level configuration.
     *
     * @param logger  the logger to configure for output
     * @param handler the existing handler to use for logging output (could be {@code null} to auto-create a
     *                {@link ConsoleHandler})
     * @param level   the logging level to set for both logger and handler
     * @throws NullPointerException if logger or level is {@code null}
     */
    public LoggingExtension(Logger logger, @Nullable Handler handler, Level level) {
        this.logger = Objects.requireNonNull(logger, "logger" + TestingUtils.CANNOT_BE_NULL);
        this.level = Objects.requireNonNull(level, "level" + TestingUtils.CANNOT_BE_NULL);
        this.handler = handler;
    }

    /**
     * Creates a LoggingExtension with a custom logger name and {@link Level#ALL}.
     *
     * @param loggerName the fully qualified logger name to configure for console output
     */
    public LoggingExtension(String loggerName) {
        this(Logger.getLogger(Objects.requireNonNull(loggerName, LOGGER_NAME_CANNOT_BE_NULL)));
    }

    /**
     * Creates a LoggingExtension with a custom logger name and logging level.
     *
     * @param loggerName the fully qualified logger name to configure for console output
     * @param level      the logging level to set for both logger and console handler
     */
    public LoggingExtension(String loggerName, Level level) {
        this(Logger.getLogger(Objects.requireNonNull(loggerName, LOGGER_NAME_CANNOT_BE_NULL)),
                Objects.requireNonNull(level, "level" + TestingUtils.CANNOT_BE_NULL));
    }

    /**
     * Creates a LoggingExtension with a custom logger name and existing handler.
     * <p>
     * The handler's existing level will be preserved. If the handler has no level explicitly set
     * (i.e. {@link Handler#getLevel()} returns {@code null}), {@link Level#ALL} is used.
     *
     * @param loggerName the fully qualified logger name to configure for output
     * @param handler    the existing handler to use for logging output
     * @throws NullPointerException if handler is {@code null}
     */
    public LoggingExtension(String loggerName, Handler handler) {
        this(Logger.getLogger(Objects.requireNonNull(loggerName, LOGGER_NAME_CANNOT_BE_NULL)),
                Objects.requireNonNull(handler, "handler" + TestingUtils.CANNOT_BE_NULL));
    }

    /**
     * Creates a LoggingExtension with a custom logger name, existing handler, and logging level override.
     *
     * @param loggerName the fully qualified logger name to configure for output
     * @param handler    the existing handler to use for logging output
     * @param level      the logging level to set for both logger and handler
     * @throws NullPointerException if handler or level is {@code null}
     */
    public LoggingExtension(String loggerName, Handler handler, Level level) {
        this(Logger.getLogger(Objects.requireNonNull(loggerName, LOGGER_NAME_CANNOT_BE_NULL)),
                Objects.requireNonNull(handler, "handler" + TestingUtils.CANNOT_BE_NULL),
                Objects.requireNonNull(level, "level" + TestingUtils.CANNOT_BE_NULL));
    }

    /**
     * Restores the original logger and handler configuration after each test method completes.
     * <p>
     * This method removes any handlers added by this extension, restores the logger's original level and parent
     * handler usage settings, and resets any modified handler levels back to their original state. If the handler
     * is a {@link TestLogHandler}, it also clears its captured log records. This ensures that both logger and
     * handler state don't leak between individual test methods.
     * <p>
     * The lookup-remove-and-possibly-drop-the-per-class-map sequence is performed as a single
     * {@link Map#compute} operation on {@code testMethodConfigs}, keyed by {@code testClass} — the same key
     * {@link #beforeEach(ExtensionContext)} uses with {@code computeIfAbsent}. {@link ConcurrentHashMap}
     * guarantees per-key atomicity between these two operations, so a concurrent {@code beforeEach} for another
     * test method in the same class can never have its freshly-added state silently dropped by this method
     * emptying and removing the shared per-class map out from under it.
     *
     * @param context the extension context providing access to the test class and unique test ID
     */
    @Override
    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "Logger.getLevel() and LoggerState.originalLevel() are nullable by design"
    )
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public void afterEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var configKey = context.getUniqueId();

        var stateRef = new AtomicReference<@Nullable LoggerState>();
        testMethodConfigs.compute(testClass, (k, methodConfigs) -> {
            if (methodConfigs == null) {
                return null;
            }
            stateRef.set(methodConfigs.remove(configKey));
            return methodConfigs.isEmpty() ? null : methodConfigs;
        });

        var state = stateRef.get();
        if (state == null) {
            return;
        }

        synchronized (state.targetLogger()) {
            if (state.addedHandler() instanceof TestLogHandler testLogHandler) {
                testLogHandler.clear();
            }

            for (var h : state.targetLogger().getHandlers()) {
                state.targetLogger().removeHandler(h);
            }

            // Only restore if there WAS an explicit original level
            // If original was null (new ConsoleHandler default), do nothing
            // This is what afterEachWithNullOriginalHandlerLevelDoesNotSetLevel asserts
            if (state.originalHandlerLevel() != null) {
                state.addedHandler().setLevel(state.originalHandlerLevel());
            }

            if (handler == null && state.addedHandler() instanceof ConsoleHandler consoleHandler) {
                consoleHandler.close();
            }

            for (var originalHandler : state.originalHandlers()) {
                state.targetLogger().addHandler(originalHandler);
            }

            state.targetLogger().setLevel(state.originalLevel());
            state.targetLogger().setUseParentHandlers(state.originalUseParentHandlers());
        }
    }

    /**
     * Configures the logger with console output before each test method runs.
     * <p>
     * This method is called before every test method. It performs the following configuration:
     *
     * <ul>
     *   <li>Uses the provided handler or creates a {@link ConsoleHandler} with the specified level</li>
     *   <li>Adds the handler to the logger</li>
     *   <li>Sets the logger's level</li>
     *   <li>Disables parent handler usage to prevent duplicate output</li>
     *   <li>Stores the original logger and handler state for restoration after the test method completes</li>
     * </ul>
     * <p>
     * The original-state snapshot and the subsequent mutation are both performed inside the same
     * {@code synchronized (logger)} block that {@link #afterEach(ExtensionContext)} uses to restore that state —
     * otherwise, when the same {@link Logger} is shared by concurrently-running test methods, the snapshot taken
     * here could race with another thread's restoration, capturing a handler list or level that's mid-change
     * rather than the true pre-test state.
     *
     * @param context the extension context providing access to the test class and unique test ID
     */
    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public void beforeEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var methodConfigs = testMethodConfigs.computeIfAbsent(testClass, k -> new ConcurrentHashMap<>());

        var handlerToUse = handler != null ? handler : new ConsoleHandler();

        LoggerState state;
        synchronized (logger) {
            // Capture BEFORE setLevel - so originalHandlerLevel can be non-null when user handler has a level
            state = new LoggerState(logger, handlerToUse);

            handlerToUse.setLevel(level);
            logger.setLevel(level);
            logger.setUseParentHandlers(false);
            logger.addHandler(handlerToUse);
        }

        methodConfigs.put(context.getUniqueId(), state);
    }

    /**
     * Holds the original state of a logger and its handlers before modification, allowing complete restoration
     * after tests complete.
     */
    private record LoggerState(
            Logger targetLogger,
            @Nullable Level originalLevel,
            boolean originalUseParentHandlers,
            Handler[] originalHandlers,
            Handler addedHandler,
            @Nullable Level originalHandlerLevel
    ) {

        LoggerState(Logger logger, Handler addedHandler) {
            this(
                    logger,
                    logger.getLevel(),
                    logger.getUseParentHandlers(),
                    logger.getHandlers().clone(),
                    Objects.requireNonNull(addedHandler),
                    addedHandler.getLevel()
            );
        }
    }
}