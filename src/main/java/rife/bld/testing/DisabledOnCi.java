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
 * JUnit annotation for disabling tests on CI/CD environments.
 *
 * <h3>Example usage:</h3>
 *
 * <blockquote><pre>
 * public class MyTest {
 *     &#64;Test
 *     &#64;DisabledOnCi
 *     public void testMethod() {
 *         // This test will be disabled on CI/CD environments
 *     }
 * }</pre></blockquote>
 *
 * <p>
 * The decision is made by checking whether the {@code CI} environment variable is defined. It can be set manually
 * but is set automatically by most CI/CD environments, such as
 * <a href="https://docs.github.com/en/actions/reference/workflows-and-actions/variables#default-environment-variables">GitHub Actions</a>,
 * <a href="https://docs.gitlab.com/ci/variables/predefined_variables/">GitLab</a>, etc.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnCiCondition.class)
public @interface DisabledOnCi {

}
