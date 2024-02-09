package org.tensortapestry.common.collections;

import javax.annotation.Nonnull;

public interface Wrapper<T> {
  @Nonnull
  T unwrap();
}
