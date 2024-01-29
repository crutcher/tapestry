package org.tensortapestry.loom.graph.dialects.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsdType {
  String value();

  @UtilityClass
  class Util {

    /**
     * Extract the type from a class.
     * @param clazz the class
     * @return the type, or null if the class is not annotated with {@link JsdType}
     */
    @Nullable public String extractType(Class<?> clazz) {
      JsdType jsdType = clazz.getAnnotation(JsdType.class);
      if (jsdType != null) {
        return jsdType.value();
      }
      return null;
    }

    /**
     * Extract the type from a class, or throw an exception if the class is not annotated.
     * @param clazz the class
     * @return the type
     */
    @Nonnull
    public String assertType(Class<?> clazz) {
      String type = extractType(clazz);
      if (type == null) {
        throw new IllegalArgumentException("Class " + clazz + " is not annotated with @JsdType");
      }
      return type;
    }
  }
}
