[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Freleases%2Fcom%2Fuwyn%2Frife2%2Fbld-testing-helpers%2Fmaven-metadata.xml&color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-testing-helpers)
[![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Fsnapshots%2Fcom%2Fuwyn%2Frife2%2Fbld-testing-helpers%2Fmaven-metadata.xml&label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-testing-helpers)
[![GitHub CI](https://github.com/rife2/bld-testing-helpers/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-testing-helpers/actions/workflows/bld.yml)

# Testing Helpers for [bld](https://github.com/rife2/bld)

To use the testing helpers, include the following in your `bld` build file:

```java
repositories = List.of(RIFE2_SNAPSHOTS, RIFE2_RELEASES, RIFE2_SNAPSHOTS);

scope(test).include(
    dependency("com.uwyn.rife2", "bld-testing-helpers", version(1, 1, 0, "SNAPSHOT"))
);
```

Please check the [documentation](https://rife2.github.io/bld-testing-helpers)
for more information.

## Annotations

The following annotations are provided:

| Annotation                                                                                                                     | Description                                                         |
|:-------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------|
| [`VisibleForTesting`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/VisibleForTesting.html)           | Indicates that a class member's visibility has been relaxed testing |



## JUnit Annotations

The following JUnit annotations are provided:

| Annotation                                                                                                              | Description                                         |
|:------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------|
| [`CaptureOutput`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/CaptureOutput.html) | Capture stdout and stderr output                    |
| [`CouldFail`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/CouldFail.html)         | Allows a test to fail                               |
| [`DisabledOnCi`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/DisabledOnCi.html)   | Disables a test when running in a CI/CD environment |
| [`EnabledOnCi`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/EnabledOnCi.html)     | Enables a test when running in a CI/CD environment  |
| [`RandomRange`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/RandomRange.html)     | Generates a random integer within a specified range |
| [`RandomString`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/RandomString.html)   | Generates a random string                           |
| [`RetryTest`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/RetryTest.html)         | Retry test on failure                               |

## JUnit Extensions

The following extensions are provided:

| Extension                                                                                                                     | Description                                |
|:------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------|
| [`LoggingExtension`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/LoggingExtension.html) | Configures console logging for test suites |

## Helpers

The following helper classes are provided:

| Helper                                                                                                                    | Description                              |
|:--------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------|
| [`TestLogHandler`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/TestLogHandler.html) | A log handler that captures log messages |

## Utilities

The following static methods are provided:

| Utility                                                                                                                                                         | Description                |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------|
| [`generateRandomInt(int, int)`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/TestingUtils.html#generateRandomInt(int,int)) | Generates a random integer |
| [`generateRandomString()`](https://rife2.github.io/bld-testing-helpers/rife/bld/extension/testing/TestingUtils.html#generateRandomString())          | Generates a random string  |
