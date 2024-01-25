package org.tensortapestry.loom.zspace.serialization;

import javax.annotation.Nonnull;
import org.tensortapestry.loom.common.json.JsonUtil;

/** An object that can be converted to a JSON string. */
public interface HasJsonOutput {
  /**
   * Convert this object to a JSON string.
   * @return the JSON string.
   */
  @Nonnull
  default String toJsonString() {
    return JsonUtil.toJson(this);
  }
}