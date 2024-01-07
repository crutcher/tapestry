package loom.common.runtime;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import lombok.NoArgsConstructor;
import loom.common.lazy.LazyString;
import org.jetbrains.annotations.Contract;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CheckThat {
  /**
   * Check that a value is not null.
   *
   * @param val the value
   * @param eClass the exception class
   * @param msg the exception message
   * @return the value
   * @param <T> the value type
   * @param <E> the exception type
   * @throws E if the value is null
   */
  @Contract("null, _, _ -> fail")
  public static <T, E extends Throwable> T valueIsNotNull(T val, Class<E> eClass, Object msg)
      throws E {
    if (val != null) {
      return val;
    }

    E exc;
    try {
      exc = eClass.getConstructor(String.class).newInstance(msg.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    throw exc;
  }

  /**
   * Check that a value is not null.
   *
   * @param val the value
   * @param eClass the exception class
   * @param format the exception message format
   * @param args the exception message format arguments
   * @return the value
   * @param <T> the value type
   * @param <E> the exception type
   * @throws E if the value is null
   */
  @FormatMethod
  @Contract("null, _, _, _ -> fail")
  public static <T, E extends Throwable> T valueIsNotNull(
      T val, Class<E> eClass, @FormatString String format, Object... args) throws E {
    return valueIsNotNull(val, eClass, LazyString.format(format, args));
  }
}
