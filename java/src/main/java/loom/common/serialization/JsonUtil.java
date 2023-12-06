package loom.common.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import javax.annotation.Nullable;
import lombok.Value;

public class JsonUtil {
  // Prevent Construction.
  private JsonUtil() {}

  /**
   * Get a Jackson ObjectMapper with default settings.
   *
   * @return the ObjectMapper.
   */
  static ObjectMapper getMapper() {
    var mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
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

  public static String reformatToPrettyJson(String json) {
    return toPrettyJson(parseToJsonNodeTree(json));
  }

  /**
   * Parse a JSON string to a Jackson JsonNode tree.
   *
   * @param json the JSON string.
   * @return the JsonNode tree.
   */
  public static JsonNode parseToJsonNodeTree(String json) {
    try {
      return getMapper().readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Convert an object to a Jackson JsonNode tree.
   *
   * @param obj the object to convert.
   * @return the JsonNode tree.
   */
  public static JsonNode valueToJsonNodeTree(Object obj) {
    return getMapper().valueToTree(obj);
  }

  public static Map<String, Object> toMap(Object obj) {
    @SuppressWarnings("unchecked")
    Map<String, Object> result = getMapper().convertValue(obj, Map.class);
    return result;
  }

  public static Map<String, Object> parseToMap(String json) {
    return toMap(parseToJsonNodeTree(json));
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
      return readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * De-serialize a JSON string to an object of the specified class.
   *
   * @param json the JSON string.
   * @param cls the class of the object to de-serialize.
   * @return the de-serialized object.
   * @param <T> the type of the object to de-serialize.
   * @throws JsonProcessingException if the JSON string cannot be de-serialized to the specified.
   */
  public static <T> T readValue(String json, Class<T> cls) throws JsonProcessingException {
    return getMapper().readValue(json, cls);
  }

  /**
   * Convert an object to an object of the specified class.
   *
   * @param tree the object to convert.
   * @param clazz the class of the object to convert to.
   * @return the converted object.
   * @param <T> the type of the object to convert to.
   * @throws IllegalArgumentException if the object cannot be converted to the specified class.
   */
  public static <T> T convertValue(Object tree, Class<T> clazz) {
    return getMapper().convertValue(tree, clazz);
  }

  /**
   * Convert an Object to a simple JSON object.
   *
   * @param obj the object to convert.
   * @return the simple JSON value tree.
   */
  public static Object toSimpleJson(Object obj) {
    return treeToSimpleJson(valueToJsonNodeTree(obj));
  }

  /**
   * Convert a Jackson JsonNode tree a simple JSON value tree.
   *
   * <p>Simple JSON value trees are composed of the following types: - null - String - Number -
   * Boolean - List<$Simple> - Map<String, $Simple>
   *
   * @param node the node to convert.
   * @return the simple JSON value tree.
   */
  public static Object treeToSimpleJson(JsonNode node) {
    if (node.isNull()) {
      return null;
    } else if (node instanceof NumericNode n) {
      return n.numberValue();
    } else if (node.isTextual()) {
      return node.textValue();
    } else if (node instanceof ArrayNode arr) {
      var result = new ArrayList<>();
      arr.elements().forEachRemaining(item -> result.add(treeToSimpleJson(item)));
      return result;
    } else if (node instanceof ObjectNode obj) {
      var result = new TreeMap<>();
      obj.fields()
          .forEachRemaining(
              field -> result.put(field.getKey(), treeToSimpleJson(field.getValue())));
      return result;
    } else {
      throw new IllegalStateException("Unexpected node type: " + node.getClass());
    }
  }

  /** Traversal context for the validateSimpleJson method. */
  @Value
  protected static class ValidatorPathContext {
    @Nullable ValidatorPathContext parent;
    @Nullable Object selector;
    @Nullable Object target;

    /**
     * Selector path from the root to this context location.
     *
     * @return a string representing the path.
     */
    String path() {
      if (selector == null) {
        return "";
      }
      var prefix = (parent == null) ? "" : parent.path();
      if (!prefix.isEmpty()) {
        prefix += ".";
      }

      if (selector instanceof String s) {
        if (!prefix.isEmpty()) {
          return prefix + "/" + s;
        } else {
          return s;
        }
      } else if (selector instanceof Number n) {
        if (!prefix.isEmpty()) {
          return prefix + "[" + n + "]";
        } else {
          throw new IllegalStateException("Unexpected value: " + selector);
        }
      } else {
        throw new IllegalStateException("Unexpected value: " + selector);
      }
    }

    /**
     * Is there a cycle in the traversal path?
     *
     * @return true if there is a cycle.
     */
    boolean isCycle() {
      var h = parent;
      while (h != null) {
        if (h.target == target) {
          return true;
        }
        h = h.parent;
      }
      return false;
    }
  }

  /**
   * Validate that a JSON object is a simple JSON value tree.
   *
   * @param tree the object to validate.
   * @throws IllegalArgumentException if the object is not a simple JSON value tree.
   */
  public static void validateSimpleJson(Object tree) {
    var scheduled = new ArrayDeque<ValidatorPathContext>();
    scheduled.add(new ValidatorPathContext(null, null, tree));

    while (!scheduled.isEmpty()) {
      var item = scheduled.pop();

      if (item.isCycle()) {
        throw new IllegalArgumentException("Cycle detected at " + item.path());
      }

      final var target = item.getTarget();

      if (target == null
          || target instanceof String
          || target instanceof Number
          || target instanceof Boolean) {
        // Valid scalar values.
        continue;
      }

      if (target instanceof Map<?, ?> map) {
        map.forEach(
            (k, v) -> {
              if (!(k instanceof String)) {
                throw new IllegalArgumentException("Unexpected key: " + k + " at " + item.path());
              }
              // Valid if all children are valid.
              scheduled.add(new ValidatorPathContext(item, k, v));
            });
        continue;
      }

      if (target instanceof List<?> list) {
        for (int i = 0; i < list.size(); i++) {
          // Valid if all children are valid.
          scheduled.add(new ValidatorPathContext(item, i, list.get(i)));
        }
        continue;
      }

      throw new IllegalArgumentException("Unexpected value type: " + target + " at " + item.path());
    }
  }
}
