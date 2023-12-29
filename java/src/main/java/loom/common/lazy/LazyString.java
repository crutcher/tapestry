package loom.common.lazy;

import com.google.errorprone.annotations.FormatMethod;
import java.util.function.Supplier;

/**
 * {@code Thunk<String>} subclass which provides {@link #toString}.
 *
 * <p>Delegates methods to the internal string product.
 */
public final class LazyString extends Thunk<String> {
  /**
   * Construct a LazyString from {#link String::format}.
   *
   * @param format the format string.
   * @param args the arguments.
   * @return a LazyString.
   */
  @FormatMethod
  public static LazyString format(String format, Object... args) {
    return new LazyString(() -> String.format(format, args));
  }

  public static LazyString fixed(String value) {
    return new LazyString(() -> value);
  }

  public LazyString(Supplier<String> supplier) {
    super(supplier);
  }
}
