package org.tensortapestry.zspace.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.msgpack.jackson.dataformat.MessagePackFactory;

@UtilityClass
public class ZSpaceJsonUtil {

  private final ObjectMapper COMMON_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  /**
   * Serialize an object from JSON via Jackson.
   *
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
   *
   * @param json the JSON string.
   * @param clazz the class of the object to deserialize.
   * @param <T> the type of the object to deserialize.
   * @return the deserialized object.
   * @throws IllegalArgumentException if the object cannot be deserialized.
   */
  @Nullable public <T> T fromJson(@Nonnull String json, @Nonnull Class<T> clazz) {
    try {
      return COMMON_MAPPER.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private final ObjectMapper MSGPACK_MAPPER = new ObjectMapper(new MessagePackFactory());

  /**
   * Serialize an object from msgpack via Jackson.
   *
   * @param obj the object to serialize.
   * @return the msgpack byte[]s.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  @Nonnull
  public byte[] toMsgPack(@Nullable Object obj) {
    try {
      return MSGPACK_MAPPER.writer().writeValueAsBytes(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Deserialize an object from msgpack via Jackson.
   *
   * @param bytes the msgpack byte[].
   * @param clazz the class of the object to deserialize.
   * @param <T> the type of the object to deserialize.
   * @return the deserialized object.
   * @throws IllegalArgumentException if the object cannot be deserialized.
   */
  @Nullable public <T> T fromMsgPack(@Nonnull byte[] bytes, @Nonnull Class<T> clazz) {
    return fromMsgPack(bytes, 0, bytes.length, clazz);
  }

  /**
   * Deserialize an object from msgpack via Jackson.
   *
   * @param bytes the msgpack byte[].
   * @param offset the offset of the byte[].
   * @param len the length of the byte[].
   * @param clazz the class of the object to deserialize.
   * @param <T> the type of the object to deserialize.
   * @return the deserialized object.
   * @throws IllegalArgumentException if the object cannot be deserialized.
   */
  @Nullable public <T> T fromMsgPack(@Nonnull byte[] bytes, int offset, int len, @Nonnull Class<T> clazz) {
    try {
      return MSGPACK_MAPPER.readValue(bytes, offset, len, clazz);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
