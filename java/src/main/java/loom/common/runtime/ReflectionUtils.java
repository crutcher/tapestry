package loom.common.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ReflectionUtils {

  /**
   * Create a new instance of a class.
   *
   * @param cls the class
   * @param args the constructor arguments
   * @return the new instance
   * @param <T> the class type
   * @throws RuntimeException if the class cannot be instantiated
   */
  @Nonnull
  public <T> T newInstance(Class<T> cls, Object... args) {
    try {
      var argClasses = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
      return cls.getConstructor(argClasses).newInstance(args);
    } catch (NoSuchMethodException
        | InvocationTargetException
        | IllegalAccessException
        | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Check that a class is a subclass of another class.
   *
   * @param cls the class
   * @param superclass the superclass
   * @throws ClassCastException if the class is not a subclass of the superclass
   */
  public void checkIsSubclass(Class<?> cls, Class<?> superclass) {
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
  public <T> Type[] getTypeArgumentsForGenericSuperclass(
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
