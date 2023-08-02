package loom.common.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.util.*;
import javax.annotation.Nullable;
import lombok.Value;
import loom.graph.LoomJacksonModule;

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
    mapper.registerModule(new LoomJacksonModule());
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

  /**
   * Is this ArrayNode an un-nested value array?
   *
   * @param node the node.
   * @return true if the node is an array of values.
   */
  public static boolean isValueArray(ArrayNode node) {
    for (JsonNode child : node) {
      if (!child.isValueNode()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Convert an Object to a simple JSON object.
   *
   * <p>Serializes to a JSON object using Jackson {@link ObjectMapper.valueToTree}; then converts
   * from {@link JsonNode} to a simple JSON value tree.
   *
   * @param obj the object to convert.
   * @return the simple JSON value tree.
   */
  public static Object toSimpleJson(Object obj) {
    return treeToSimpleJson(toTree(obj));
  }

  /**
   * Convert a Jackson JsonNode tree a a simple JSON value tree.
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

  /**
   * Validate that a JSON object is a simple JSON value tree.
   *
   * @param obj the object to validate.
   * @throws IllegalArgumentException if the object is not a simple JSON value tree.
   */
  public static void validateSimpleJson(Object obj) {
    @Value
    class History {
      @Nullable History parent;
      @Nullable Object selector;
      @Nullable Object target;

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

    var toVisit = new ArrayDeque<History>();
    toVisit.add(new History(null, null, obj));
    while (!toVisit.isEmpty()) {
      var item = toVisit.pop();
      if (item.target == null) {
        continue;
      }

      if (item.isCycle()) {
        throw new IllegalArgumentException("Cycle detected at " + item.path());
      }
      var target = item.getTarget();
      if (target instanceof String) {
        continue;
      } else if (target instanceof Number) {
        continue;
      } else if (target instanceof Boolean) {
        continue;
      } else if (target instanceof Map<?, ?> map) {
        for (var entry : map.entrySet()) {
          var key = entry.getKey();
          var value = entry.getValue();
          if (!(key instanceof String)) {
            throw new IllegalArgumentException("Unexpected key: " + key + " at " + item.path());
          }
          toVisit.add(new History(item, key, value));
        }
      } else if (target instanceof List<?> list) {
        int i = 0;
        for (var entry : list) {
          toVisit.add(new History(item, i++, entry));
        }
      } else {
        throw new IllegalArgumentException(
            "Unexpected value type: " + target + " at " + item.path());
      }
    }
  }
}
