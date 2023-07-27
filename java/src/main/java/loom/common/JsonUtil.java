package loom.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.util.Map;
import loom.alt.attrgraph.LoomGraph;

public class JsonUtil {
  // Prevent Construction.
  private JsonUtil() {}

  /**
   * Get a Jackson ObjectMapper with default settings.
   *
   * @return the ObjectMapper.
   */
  private static ObjectMapper getMapper() {
    var mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    mapper.registerModule(new LoomGraph.JsonSupport.LoomGraphModule());
    return mapper;
  }

  /**
   * Serialize an object to JSON via Jackson defaults.
   *
   * @param obj the object to serialize.
   * @return the JSON string.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  public static String toJson(Object obj) {
    try {
      return getMapper().writer().writeValueAsString(obj);
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
    try {
      return getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String toXml(Object obj) {
    var mapper = new XmlMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String reformat(String json) {
    return toPrettyJson(readTree(json));
  }

  public static JsonNode readTree(String json) {
    try {
      return getMapper().readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static JsonNode toTree(Object obj) {
    return getMapper().valueToTree(obj);
  }

  public static Map<String, Object> toMap(Object obj) {
    @SuppressWarnings("unchecked")
    Map<String, Object> result = getMapper().convertValue(obj, Map.class);
    return result;
  }

  /**
   * De-serialize a JSON string to an object of the specified class.
   *
   * @param json the JSON string.
   * @param clazz the class of the object to de-serialize.
   * @param <T> the type of the object to de-serialize.
   * @return the de-serialized object.
   * @throws IllegalArgumentException if the JSON string cannot be de-serialized to the specified
   *     class.
   */
  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return getMapper().readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static <T> T fromJson(JsonNode node, Class<T> clazz) {
    try {
      return getMapper().treeToValue(node, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static <T> T roundtrip(T obj) {
    @SuppressWarnings("unchecked")
    var cls = (Class<T>) obj.getClass();
    return fromJson(toJson(obj), cls);
  }
}
