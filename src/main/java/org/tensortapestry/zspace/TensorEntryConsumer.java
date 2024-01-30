package org.tensortapestry.zspace;

/** A consumer which accepts a coordinate array and a value. */
@FunctionalInterface
public interface TensorEntryConsumer {
  /**
   * Accept a coordinate array and a value.
   *
   * @param coords the coordinate array.
   * @param value the value.
   */
  void accept(int[] coords, int value);
}
