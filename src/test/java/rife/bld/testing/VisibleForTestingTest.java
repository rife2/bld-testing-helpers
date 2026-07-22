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

import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

class VisibleForTestingTest {

    @Test
    void annotationHasClassRetention() {
        Retention retention = VisibleForTesting.class.getAnnotation(Retention.class);
        assertNotNull(retention, "@VisibleForTesting should have @Retention");
        assertEquals(RetentionPolicy.CLASS, retention.value());
    }

    @Test
    void annotationIsAnAnnotationType() {
        assertTrue(VisibleForTesting.class.isAnnotation());
    }

    @Test
    void annotationIsDocumented() {
        assertNotNull(
                VisibleForTesting.class.getAnnotation(Documented.class),
                "@VisibleForTesting should be @Documented"
        );
    }

    @Test
    void annotationTargetDeclarationCoversAllFourTargets() {
        Target target = VisibleForTesting.class.getAnnotation(Target.class);
        assertNotNull(target, "@VisibleForTesting should have @Target");

        var targets = java.util.Arrays.asList(target.value());
        assertAll(
                () -> assertTrue(targets.contains(ElementType.METHOD), "Should target METHOD"),
                () -> assertTrue(targets.contains(ElementType.CONSTRUCTOR), "Should target CONSTRUCTOR"),
                () -> assertTrue(targets.contains(ElementType.FIELD), "Should target FIELD"),
                () -> assertTrue(targets.contains(ElementType.TYPE), "Should target TYPE")
        );
    }
}