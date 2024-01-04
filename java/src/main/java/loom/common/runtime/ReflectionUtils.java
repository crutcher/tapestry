package loom.common.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ReflectionUtils {

  public static void checkIsSubclass(Class<?> cls, Class<?> superclass) {
    if (!superclass.isAssignableFrom(cls)) {
      throw new ClassCastException(cls + " is not a subclass of " + superclass);
    }
  }

  /**
   * Get the type arguments for the generic superclass ancestor of a class.
   *
   * @param cls the class
   * @param superclass the superclass ancestor
   * @return the type arguments
   * @param <T> the class type
   */
  public static <T> Type[] getTypeArgumentsForGenericSuperclass(
      Class<T> cls, Class<? super T> superclass) {
    Class<?> cur = cls;
    while (true) {
      if (cur == null) {
        throw new IllegalArgumentException(cls + " is not a subclass of " + superclass);
      }
      var next = cur.getSuperclass();
      if (next == superclass) {
        break;
      }
      cur = next;
    }

    var pt = (ParameterizedType) cur.getGenericSuperclass();
    return pt.getActualTypeArguments();
  }
}
