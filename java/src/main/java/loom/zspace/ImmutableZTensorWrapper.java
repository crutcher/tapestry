package loom.zspace;

import com.fasterxml.jackson.annotation.JsonValue;
import loom.common.json.HasToJsonString;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Base class for immutable wrappers around ZTensors.
 *
 * @param <T> the type of the subclass.
 */
@ThreadSafe
public abstract class ImmutableZTensorWrapper<T> implements HasZTensor, Cloneable, HasToJsonString {
  @JsonValue
  @Nonnull
  @SuppressWarnings("Immutable")
  ZTensor tensor;

  public ImmutableZTensorWrapper(HasZTensor tensor) {
    this.tensor = tensor.asZTensor().asImmutable();
  }

  protected abstract T create(HasZTensor tensor);

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
   * Is {@code this == rhs}?
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

  public int sumAsInt() {
    return tensor.sumAsInt();
  }

  public int prodAsInt() {
    return tensor.prodAsInt();
  }

  public int maxAsInt() {
    return tensor.maxAsInt();
  }

  public int minAsInt() {
    return tensor.minAsInt();
  }

  public T neg() {
    return create(ZTensorOperations.neg(this));
  }

  public T add(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.add(this, rhs));
  }

  public T add(int rhs) {
    return create(ZTensorOperations.add(this, rhs));
  }

  public T sub(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.sub(this, rhs));
  }

  public T sub(int rhs) {
    return create(ZTensorOperations.sub(this, rhs));
  }

  public T mul(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.mul(this, rhs));
  }

  public T mul(int rhs) {
    return create(ZTensorOperations.mul(this, rhs));
  }

  public T div(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.div(this, rhs));
  }

  public T div(int rhs) {
    return create(ZTensorOperations.div(this, rhs));
  }

  public T mod(@Nonnull HasZTensor rhs) {
    return create(ZTensorOperations.mod(this, rhs));
  }

  public T mod(int rhs) {
    return create(ZTensorOperations.mod(this, rhs));
  }
}
