package loom.common.runtime;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ReflectionUtils {

  public static void checkIsSubclass(Class<?> cls, Class<?> superclass) {
    if (!superclass.isAssignableFrom(cls)) {
      throw new ClassCastException(cls + " is not a subclass of " + superclass);
    }
  }
}
