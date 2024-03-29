package org.tensortapestry.common.runtime;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.tensortapestry.common.lazy.LazyString;

@UtilityClass
public class CheckThat {

  /**
   * Check that a value is not null.
   *
   * @param val the value
   * @param eClass the exception class
   * @param msg the exception message
   * @param <T> the value type
   * @param <E> the exception type
   * @return the value
   * @throws E if the value is null
   */
  @Contract("null, _, _ -> fail")
  @CanIgnoreReturnValue
  public <T, E extends Throwable> T valueIsNotNull(T val, Class<E> eClass, Object msg) throws E {
    if (val != null) {
      return val;
    }

    throw ReflectionUtils.newInstance(eClass, msg.toString());
  }

  /**
   * Check that a value is not null.
   *
   * @param val the value
   * @param eClass the exception class
   * @param format the exception message format
   * @param args the exception message format arguments
   * @param <T> the value type
   * @param <E> the exception type
   * @return the value
   * @throws E if the value is null
   */
  @FormatMethod
  @Contract("null, _, _, _ -> fail")
  @CanIgnoreReturnValue
  public <T, E extends Throwable> T valueIsNotNull(
    T val,
    Class<E> eClass,
    @FormatString final String format,
    final Object... args
  ) throws E {
    return valueIsNotNull(val, eClass, LazyString.format(format, args));
  }
}
