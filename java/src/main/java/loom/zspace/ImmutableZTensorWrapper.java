package loom.zspace;

import com.fasterxml.jackson.annotation.JsonValue;
import loom.common.json.HasToJsonString;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public abstract class ImmutableZTensorWrapper<T> implements HasZTensor, Cloneable, HasToJsonString {
  @JsonValue
  @Nonnull
  @SuppressWarnings("Immutable")
  ZTensor tensor;

  public ImmutableZTensorWrapper(HasZTensor tensor) {
    this.tensor = tensor.asZTensor().asImmutable();
  }

  @SuppressWarnings("unchecked")
  private T self() {
    return (T) this;
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public final T clone() {
    // Immutable, so no need to clone.
    return self();
  }

  @Override
  public final ZTensor asZTensor() {
    return tensor;
  }

  @Override
  public final int hashCode() {
    return tensor.hashCode();
  }

  @Override
  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  public final boolean equals(Object other) {
    return tensor.equals(other);
  }

  @Override
  public final String toString() {
    return tensor.toString();
  }

  /**
   * Is `this == rhs`?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean eq(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.eq(this, rhs);
  }

  /**
   * Is {@code this != rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean ne(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.ne(this, rhs);
  }

  /**
   * Is {@code this < rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean lt(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.lt(this, rhs);
  }

  /**
   * Is {@code this <= rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean le(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.le(this, rhs);
  }

  /**
   * Is {@code this > rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean gt(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.gt(this, rhs);
  }

  /**
   * Is {@code this >= rhs}?
   *
   * @param rhs the right-hand side.
   * @return true or false.
   */
  public final boolean ge(@Nonnull HasZTensor rhs) {
    return DominanceOrderingOperations.ge(this, rhs);
  }
}
