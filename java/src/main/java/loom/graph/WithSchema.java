package loom.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation for specifying a JSON schema for a class. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithSchema {
  String value();
}
