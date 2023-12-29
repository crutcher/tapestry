package loom.common.lazy;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.SneakyThrows;

/**
 * A lazy {@code Supplier<T>}.
 *
 * <p>Value is computed once on the first call to {#link Thunk::get}.
 *
 * @param <T> the type of the Thunk.
 */
public class Thunk<T> implements Supplier<T> {
  /**
   * Construct a Thunk from a Supplier.
   *
   * @param supplier the supplier.
   * @param <T> the type of the Thunk.
   * @return a Thunk.
   */
  public static <T> Thunk<T> of(Supplier<T> supplier) {
    return new Thunk<>(supplier);
  }

  /**
   * Construct a Thunk from a fixed value.
   *
   * @param value the value.
   * @param <T> the type of the Thunk.
   * @return a Thunk.
   */
  public static <T> Thunk<T> fixed(T value) {
    return new Thunk<>(() -> value);
  }

  @Nullable private Supplier<T> supplier;
  @Nullable private Throwable error;

  @Nullable private T value;

  public Thunk(@Nonnull Supplier<T> supplier) {
    this.supplier = supplier;
  }

  @Override
  @SneakyThrows
  public synchronized T get() {
    if (supplier != null) {
      try {
        value = supplier.get();
      } catch (Throwable t) {
        error = t;
      }
      supplier = null;
    }
    if (error != null) {
      throw error;
    }
    return value;
  }

  @Override
  public String toString() {
    return get().toString();
  }
}
