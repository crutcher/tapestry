package loom.common.runtime;

import java.io.InputStream;

public final class ReflectionUtils {
  private ReflectionUtils() {}

  public static void checkIsSubclass(Class<?> cls, Class<?> superclass) {
    if (!superclass.isAssignableFrom(cls)) {
      throw new ClassCastException(cls + " is not a subclass of " + superclass);
    }
  }

  public static InputStream resourceAsStream(String path) {
    return resourceAsStream(ReflectionUtils.class, path);
  }

  public static InputStream resourceAsStream(Class<?> cls, String path) {
    return cls.getClassLoader().getResourceAsStream(path);
  }
}
