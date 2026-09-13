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

package rife.bld;

import rife.bld.extension.JUnitReporterOperation;
import rife.bld.extension.PmdOperation;
import rife.bld.extension.SpotBugsOperation;
import rife.bld.extension.tools.IOTools;
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishLicense;
import rife.bld.publish.PublishScm;

import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.provided;
import static rife.bld.dependencies.Scope.test;
import static rife.bld.operations.JavadocOptions.DocLinkOption.NO_MISSING;

public class TestingHelpersBuild extends Project {

    public TestingHelpersBuild() {
        pkg = "rife.bld";
        name = "Testing Helpers";
        archiveBaseName = "bld-testing-helpers";
        version = version(1, 1, 2, "SNAPSHOT");

        javaRelease = 17;

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES, RIFE2_SNAPSHOTS);

        var junit = version(6, 1, 3);
        var junitJupiter = dependency("org.junit.jupiter", "junit-jupiter", junit);
        var junitPlatform =
                dependency("org.junit.platform", "junit-platform-console-standalone", junit);

        scope(provided)
                .include(junitJupiter)
                .include(junitPlatform)
                .include(dependency("com.github.spotbugs", "spotbugs-annotations",
                        version(4, 10, 4)));
        scope(test)
                .include(dependency("org.jspecify", "jspecify", "1.0.1"))
                .include(junitJupiter)
                .include(junitPlatform)
                .include(dependency("io.github.classgraph", "classgraph",
                        version(4, 8, 195)))
                .include(dependency("org.mockito", "mockito-junit-jupiter",
                        version(5, 23, 0)));

        javadocOperation()
                .javadocOptions()
                .author()
                .docLint(NO_MISSING)
                .link("https://docs.junit.org/current/api/")
                .link("https://jspecify.dev/docs/api/");

        publishOperation()
                .repository(version.isSnapshot() ?
                        repository("rife2-snapshot") : repository("rife2"))
                .repository(repository("github"))
                .info()
                .groupId("com.uwyn.rife2")
                .artifactId(archiveBaseName)
                .description("Testing Helpers for bld")
                .url("https://github.com/rife2/" + archiveBaseName)
                .developer(new PublishDeveloper()
                        .id("ethauvin")
                        .name("Erik C. Thauvin")
                        .email("erik@thauvin.net")
                        .url("https://erik.thauvin.net/")
                )
                .license(new PublishLicense()
                        .name("The Apache License, Version 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.txt")
                )
                .scm(new PublishScm()
                        .connection("scm:git:https://github.com/rife2/" + archiveBaseName + ".git")
                        .developerConnection("scm:git:git@github.com:rife2/" + archiveBaseName + ".git")
                        .url("https://github.com/rife2/" + archiveBaseName)
                )
                .signKey(property("sign.key"))
                .signPassphrase(property("sign.passphrase"));

        compileOperation().compileOptions().encoding("UTF-8");
    }

    @Override
    public void test() throws Exception {
        var op = testOperation().fromProject(this);
        // Set the reports directory
        op.testToolOptions().reportsDir(IOTools.resolveFile(buildDirectory(), "test-results", "test"));
        op.execute();
    }

    public static void main(String[] args) {
        new TestingHelpersBuild().start(args);
    }

    @BuildCommand(summary = "Runs PMD analysis")
    public void pmd() throws Exception {
        new PmdOperation()
                .failOnViolation(true)
                .ruleSets("config/pmd.xml")
                .fromProject(this)
                .execute();
    }

    @BuildCommand(summary = "Runs the JUnit reporter")
    public void reporter() throws Exception {
        new JUnitReporterOperation()
                .fromProject(this)
                .failOnSummary(true)
                .execute();
    }

    @BuildCommand(summary = "Runs SpotBugs on this project")
    public void spotbugs() throws Exception {
        new SpotBugsOperation()
                .fromProject(this)
                .home("/opt/spotbugs")
                .execute();
    }
}
