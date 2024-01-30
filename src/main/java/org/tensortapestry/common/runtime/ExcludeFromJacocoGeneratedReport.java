package org.tensortapestry.common.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exclude a method from the jacoco generated report.
 *
 * <p>See: <a
 * href="https://stackoverflow.com/questions/47824761/how-would-i-add-an-annotation-to-exclude-a-method-from-a-jacoco-code-coverage-re">Stack
 * Overflow</a> See: <a href="https://github.com/jacoco/jacoco/pull/822/files">GitHub Pull
 * Request</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExcludeFromJacocoGeneratedReport {
}
