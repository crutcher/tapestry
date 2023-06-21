package loom.common;

public final class ReflectionUtils {
  private ReflectionUtils() {}

  public static void checkIsSubclass(Class<?> cls, Class<?> superclass) {
    if (!superclass.isAssignableFrom(cls)) {
      throw new ClassCastException(cls + " is not a subclass of " + superclass);
    }
  }
}
