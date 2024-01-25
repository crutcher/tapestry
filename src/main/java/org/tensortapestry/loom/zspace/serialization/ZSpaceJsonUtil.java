package org.tensortapestry.loom.zspace.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ZSpaceJsonUtil {

  private final ObjectMapper COMMON_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  public String toJson(Object obj) {
    try {
      return COMMON_MAPPER.writer().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public <T> T fromJson(String json, Class<T> clazz) {
    try {
      return COMMON_MAPPER.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
