package org.tensortapestry.common.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JsonViewWrapper {

  public static JsonViewWrapper of(Object value) {
    return new JsonViewWrapper(value);
  }

  @Nullable private JsonNode jsonValue;

  @JsonIgnore
  @Nullable private Object objectValue;

  @JsonCreator
  public JsonViewWrapper(Object value) {
    setValue(value);
  }

  public synchronized void setValue(Object value) {
    this.jsonValue = null;
    this.objectValue = null;
    if (value instanceof JsonNode jsonNode) {
      this.jsonValue = jsonNode;
    } else {
      this.objectValue = value;
    }
  }

  public boolean isNull() {
    if (jsonValue != null) {
      return jsonValue.isNull();
    }
    return objectValue == null;
  }

  private JsonNode _asTree() {
    if (objectValue != null) {
      return JsonUtil.convertValue(objectValue, JsonNode.class);
    }
    return jsonValue;
  }

  @Nonnull
  @JsonValue
  @CanIgnoreReturnValue
  public synchronized JsonNode viewAsJsonNode() {
    if (objectValue != null) {
      setValue(_asTree());
    }
    assert jsonValue != null;
    return jsonValue;
  }

  @SuppressWarnings("unchecked")
  public synchronized <T> T viewAs(@Nonnull Class<T> clazz) {
    if (objectValue != null && clazz != objectValue.getClass()) {
      viewAsJsonNode();
    }
    if (jsonValue != null) {
      try {
        setValue(JsonUtil.convertValue(jsonValue, clazz));
      } catch (Exception e) {
        throw new ViewConversionError("Failed to convert " + jsonValue + " to " + clazz, e);
      }
    }
    return (T) objectValue;
  }

  @Override
  public synchronized boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JsonViewWrapper wrapper)) return false;
    return _asTree().equals(wrapper._asTree());
  }

  @Override
  public int hashCode() {
    return _asTree().hashCode();
  }
}
