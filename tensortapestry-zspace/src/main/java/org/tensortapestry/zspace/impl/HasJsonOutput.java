package org.tensortapestry.zspace.impl;

import javax.annotation.Nonnull;

/**
 * An object that can be converted to a JSON string.
 */
public interface HasJsonOutput {
  /**
   * Convert this object to a JSON string.
   *
   * @return the JSON string.
   */
  @Nonnull
  default String toJsonString() {
    return ZSpaceJsonUtil.toJson(this);
  }
}
