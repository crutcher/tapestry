package org.tensortapestry.loom.zspace.serialization;

import org.tensortapestry.loom.common.json.JsonUtil;

/** An object that can be converted to a JSON string. */
public interface HasJsonOutput {
  /**
   * Convert this object to a JSON string.
   * @return the JSON string.
   */
  default String toJsonString() {
    return JsonUtil.toJson(this);
  }

  /**
   * Convert this object to a pretty JSON string.
   *
   * @return the pretty JSON string.
   */
  default String toPrettyJsonString() {
    return JsonUtil.toPrettyJson(this);
  }
}
