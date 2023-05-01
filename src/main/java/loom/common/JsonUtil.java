package loom.common;

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
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    try {
      return mapper.writer().writeValueAsString(obj);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * De-seralize a JSON string to an object of the specified class.
   *
   * @param json the JSON string.
   * @param clazz the class of the object to de-serialize.
   * @return the de-serialized object.
   * @param <T> the type of the object to de-serialize.
   * @throws IllegalArgumentException if the JSON string cannot be de-serialized to the specified
   *     class.
   */
  public static <T> T fromJson(String json, Class<T> clazz) {
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    try {
      return mapper.readValue(json, clazz);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
