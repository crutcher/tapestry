package org.tensortapestry.zspace.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ZSpaceJsonUtil {

  private final ObjectMapper COMMON_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  /**
   * Serialize an object from JSON via Jackson defaults.
   * @param obj the object to serialize.
   * @return the JSON string.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  @Nonnull
  public String toJson(@Nullable Object obj) {
    try {
      return COMMON_MAPPER.writer().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Serialize an object to pretty JSON via Jackson defaults.
   *
   * @param obj the object to serialize.
   * @return the pretty JSON string.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  public String toPrettyJson(Object obj) {
    try {
      return COMMON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Deserialize an object from JSON via Jackson defaults.
   * @param json the JSON string.
   * @param clazz the class of the object to deserialize.
   * @return the deserialized object.
   * @param <T> the type of the object to deserialize.
   * @throws IllegalArgumentException if the object cannot be deserialized.
   */
  @Nullable public <T> T fromJson(@Nonnull String json, @Nonnull Class<T> clazz) {
    try {
      return COMMON_MAPPER.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
