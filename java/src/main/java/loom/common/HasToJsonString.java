package loom.common;

public interface HasToJsonString {
  /** Convert this object to a JSON string. */
  default String toJsonString() {
    return JsonUtil.toJson(this);
  }

  default String toPrettyJsonString() {
    return JsonUtil.toPrettyJson(this);
  }
}
