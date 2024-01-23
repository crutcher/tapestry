package org.tensortapestry.loom.zspace;

/** A consumer which accepts a coordinate array and a value. */
@FunctionalInterface
public interface TensorEntryConsumer {
  void accept(int[] coords, int value);
}
