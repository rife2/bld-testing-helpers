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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Indicates that the visibility of a type or member has been relaxed
 * to make the code testable.
 *
 * <p>Members annotated with {@code @VisibleForTesting} are intended to be
 * {@code private} or otherwise less visible, but have been made package-private,
 * {@code protected}, or {@code public} to allow access from unit tests.
 *
 * <p>This annotation is intended only as documentation. Production code should
 * never call a method annotated with {@code @VisibleForTesting} outside of tests.
 *
 * @since 1.0
 */
@Documented
@Retention(CLASS)
@Target({METHOD, CONSTRUCTOR, FIELD, TYPE})
public @interface VisibleForTesting {

}
