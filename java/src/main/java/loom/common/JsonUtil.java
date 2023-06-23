package loom.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JsonUtil {
  // Prevent Construction.
  private JsonUtil() {}

  /**
   * Serialize an object to JSON via Jackson defaults.
   *
   * @param obj the object to serialize.
   * @return the JSON string.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  public static String toJson(Object obj) {
    var mapper = new ObjectMapper();
    try {
      return mapper.writer().writeValueAsString(obj);
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
  public static String toPrettyJson(Object obj) {
    var mapper = new ObjectMapper();
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static Map<String, Object> toMap(Object obj) {
    var mapper = new ObjectMapper();
    @SuppressWarnings("unchecked")
    Map<String, Object> result = mapper.convertValue(obj, Map.class);
    return result;
  }

  /**
   * De-seralize a JSON string to an object of the specified class.
   *
   * @param json the JSON string.
   * @param clazz the class of the object to de-serialize.
   * @param <T> the type of the object to de-serialize.
   * @return the de-serialized object.
   * @throws IllegalArgumentException if the JSON string cannot be de-serialized to the specified
   *     class.
   */
  public static <T> T fromJson(String json, Class<T> clazz) {
    var mapper = new ObjectMapper();
    try {
      return mapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
