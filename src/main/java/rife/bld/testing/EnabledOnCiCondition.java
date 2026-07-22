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

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Implementation of JUnit's {@link ExecutionCondition} to conditionally enable tests when running in a CI/CD
 * environment.
 * <p>
 * This condition determines whether a test or test container should be enabled or disabled based on the presence of
 * the {@code CI} environment variable. If the variable is set, the condition enables the test; otherwise, it disables
 * the test.
 * <p>
 * The presence of the {@code CI} environment variable is typically an indicator that the code is running in a CI/CD
 * pipeline, as this variable is automatically defined in most CI environments such as GitHub Actions, GitLab CI, and
 * others.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class EnabledOnCiCondition implements ExecutionCondition {

    /**
     * Evaluates whether the execution of a test should be enabled or disabled based on the CI environment.
     * <p>
     * Returns an enabled state if the test is running in a CI/CD environment, otherwise returns a disabled state.
     *
     * @param context the {@link ExtensionContext} for the current test or container
     * @return a {@link ConditionEvaluationResult} indicating whether the test is enabled or disabled
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (DisabledOnCiCondition.isCi()) {
            return ConditionEvaluationResult.enabled("Test enabled in CI/CD environment.");
        } else {
            return ConditionEvaluationResult.disabled("Test disabled in non CI/CD environment.");
        }
    }
}
