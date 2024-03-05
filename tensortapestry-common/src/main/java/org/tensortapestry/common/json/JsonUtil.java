package org.tensortapestry.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.msgpack.jackson.dataformat.MessagePackFactory;

@UtilityClass
public final class JsonUtil {

  @UtilityClass
  public class Tree {

    /**
     * Check if all elements in an array are numeric.
     *
     * @param array The array to check.
     * @return True if all elements are numeric, false otherwise.
     */
    public boolean isAllNumeric(ArrayNode array) {
      return allOf(array, JsonNode::isNumber);
    }

    /**
     * Check if all elements in an array match the Predicate.
     *
     * @param array The array to check.
     * @return True if all elements are boolean, false otherwise.
     */
    public boolean allOf(ArrayNode array, Predicate<JsonNode> predicate) {
      for (var it = array.elements(); it.hasNext();) {
        var node = it.next();
        if (!predicate.test(node)) {
          return false;
        }
      }
      return true;
    }

    /**
     * Check if any elements in an array match the Predicate.
     *
     * @param array The array to check.
     * @return True if any elements are numeric, false otherwise.
     */
    public boolean anyOf(ArrayNode array, Predicate<JsonNode> predicate) {
      for (var it = array.elements(); it.hasNext();) {
        var node = it.next();
        if (predicate.test(node)) {
          return true;
        }
      }
      return false;
    }

    /**
     * Adapt an ArrayNode to a {@code Stream<JsonNode>}.
     *
     * @param array The array to adapt.
     * @return The Stream.
     */
    public Stream<JsonNode> stream(ArrayNode array) {
      return StreamSupport.stream(array.spliterator(), false);
    }

    /**
     * Adapt an ObjectNode to a {@code Stream<Map.Entry<String, JsonNode>>}.
     *
     * @param object The object to adapt.
     * @return The Stream.
     */
    public Stream<Map.Entry<String, JsonNode>> entryStream(ObjectNode object) {
      Iterable<Map.Entry<String, JsonNode>> entries = object::fields;
      return StreamSupport.stream(entries.spliterator(), false);
    }
  }

  // /**
  //  * Format a parse error.
  //  *
  //  * @param e The exception.
  //  * @param source The source string.
  //  * @return The formatted error.
  //  */
  // static String formatParseError(JsonParsingException e, String source) {
  //   StringBuilder sb = new StringBuilder();
  //   sb.append(e.getMessage()).append("\n");
  //   var location = e.getLocation();
  //   var k = location.getLineNumber() - 1;
  //   var lines = Splitter.on("\n").splitToList(source);
  //   for (int i = 0; i < lines.size(); i++) {
  //     if (i == k) {
  //       sb.append(">>> ");
  //     } else {
  //       sb.append("    ");
  //     }
  //     sb.append(lines.get(i)).append("\n");
  //   }
  //   return sb.toString();
  // }

  private final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.instance;

  private final ObjectMapper COMMON_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  private final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  private final Configuration JSON_PATH_CONFIG = Configuration
    .builder()
    .jsonProvider(new JacksonJsonNodeJsonProvider())
    .mappingProvider(new JacksonMappingProvider())
    .build();

  public interface WithNodeBuilders {
    @Nonnull
    default NullNode nullNode() {
      return JSON_NODE_FACTORY.nullNode();
    }

    @Nonnull
    default JsonNode missingNode() {
      return JSON_NODE_FACTORY.missingNode();
    }

    @Nonnull
    default BooleanNode booleanNode(boolean value) {
      return JSON_NODE_FACTORY.booleanNode(value);
    }

    @Nonnull
    default NumericNode numberNode(int v) {
      return JSON_NODE_FACTORY.numberNode(v);
    }

    @Nonnull
    default NumericNode numberNode(float v) {
      return JSON_NODE_FACTORY.numberNode(v);
    }

    @Nonnull
    default NumericNode numberNode(double v) {
      return JSON_NODE_FACTORY.numberNode(v);
    }

    @Nonnull
    default TextNode textNode(String value) {
      return JSON_NODE_FACTORY.textNode(value);
    }

    @Nonnull
    default ObjectNode objectNode() {
      return JSON_NODE_FACTORY.objectNode();
    }

    @Nonnull
    default ArrayNode arrayNode() {
      return JSON_NODE_FACTORY.arrayNode();
    }
  }

  /**
   * Get a Jackson ObjectMapper with default settings.
   *
   * @return the ObjectMapper.
   */
  public ObjectMapper getObjectMapper() {
    return COMMON_MAPPER;
  }

  /**
   * Construct a JsonPath ParseContext bound to our default configuration.
   *
   * @return a new ParseContext.
   */
  public ParseContext jsonPathParseContext() {
    return JsonPath.using(JSON_PATH_CONFIG);
  }

  /**
   * Evaluate a JsonPath expression on a node.
   *
   * @param node the node to evaluate the expression on.
   * @param path the JsonPath expression.
   * @param cls the class of the result.
   * @param <T> the type of the result.
   * @return the result.
   */
  public <T> T jsonPathOnValue(Object node, String path, Class<T> cls) {
    return jsonPathParseContext().parse(node).read(path, cls);
  }

  /**
   * Evaluate a JsonPath expression on a node.
   *
   * @param node the node to evaluate the expression on.
   * @param path the JsonPath expression.
   * @param type the type of the result.
   * @param <T> the type of the result.
   * @return the result.
   */
  public <T> T jsonPathOnValue(Object node, String path, TypeRef<T> type) {
    return jsonPathParseContext().parse(node).read(path, type);
  }

  /**
   * Serialize an object to JSON via Jackson defaults.
   *
   * @param obj the object to serialize.
   * @return the JSON string.
   * @throws IllegalArgumentException if the object cannot be serialized.
   */
  public String toJson(Object obj) {
    try {
      return getObjectMapper().writer().writeValueAsString(obj);
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
      return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public String reformatToPrettyJson(String json) {
    return toPrettyJson(parseToJsonNodeTree(json));
  }

  public String toYaml(Object obj) {
    try {
      return YAML_MAPPER.writer().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Parse a JSON string to a Jackson JsonNode tree.
   *
   * @param json the JSON string.
   * @return the JsonNode tree.
   */
  public JsonNode parseToJsonNodeTree(String json) {
    try {
      return getObjectMapper().readTree(json);
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
  public JsonNode valueToJsonNodeTree(Object obj) {
    return getObjectMapper().valueToTree(obj);
  }

  /**
   * De-serialize a JSON string to an object of the specified class.
   *
   * @param json the JSON string.
   * @param clazz the class of the object to de-serialize.
   * @param <T> the type of the object to de-serialize.
   * @return the de-serialized object.
   * @throws IllegalArgumentException if the JSON string cannot be de-serialized to the specified
   *   class.
   */
  public <T> T fromJson(String json, Class<T> clazz) {
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
   * @param <T> the type of the object to de-serialize.
   * @return the de-serialized object.
   * @throws JsonProcessingException if the JSON string cannot be de-serialized to the specified.
   */
  public <T> T readValue(String json, Class<T> cls) throws JsonProcessingException {
    return getObjectMapper().readValue(json, cls);
  }

  /**
   * Convert an object to an object of the specified class.
   *
   * @param tree the object to convert.
   * @param clazz the class of the object to convert to.
   * @param <T> the type of the object to convert to.
   * @return the converted object.
   * @throws IllegalArgumentException if the object cannot be converted to the specified class.
   */
  public <T> T convertValue(Object tree, Class<T> clazz) {
    return getObjectMapper().convertValue(tree, clazz);
  }

  /**
   * Convert an Object to a simple JSON object.
   *
   * @param obj the object to convert.
   * @return the simple JSON value tree.
   */
  public Object toSimpleJson(Object obj) {
    return treeToSimpleJson(valueToJsonNodeTree(obj));
  }

  /**
   * Convert a Jackson JsonNode tree a simple JSON value tree.
   *
   * <p>Simple JSON value trees are composed of the following types:
   *
   * <ul>
   *   <li>{@code null}
   *   <li>{@code String}
   *   <li>{@code Number}
   *   <li>{@code Boolean}
   *   <li>{@code List<Simple>}
   *   <li>{@code Map<String, Simple>}
   * </ul>
   *
   * @param node the node to convert.
   * @return the simple JSON value tree.
   */
  public Object treeToSimpleJson(JsonNode node) {
    if (node.isNull()) {
      return null;
    } else if (node instanceof BooleanNode) {
      return node.booleanValue();
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
      obj
        .fields()
        .forEachRemaining(field -> result.put(field.getKey(), treeToSimpleJson(field.getValue())));
      return result;
    } else {
      throw new IllegalArgumentException("Unexpected node type: " + node.getClass());
    }
  }

  /**
   * Traversal context for the validateSimpleJson method.
   */
  @Value
  protected class SelectionPath {

    @Nullable SelectionPath parent;

    @Nullable Object selector;

    @Nullable Object target;

    public SelectionPath(@Nullable Object target) {
      this.parent = null;
      this.selector = null;
      this.target = target;
    }

    @SuppressWarnings("InconsistentOverloads")
    public SelectionPath(
      @Nullable SelectionPath parent,
      @Nullable String selector,
      @Nullable Object target
    ) {
      this.parent = parent;
      this.selector = selector;
      this.target = target;
    }

    @SuppressWarnings("InconsistentOverloads")
    public SelectionPath(
      @Nullable SelectionPath parent,
      @Nullable Integer selector,
      @Nullable Object target
    ) {
      this.parent = parent;
      this.selector = selector;
      this.target = target;
    }

    /**
     * Selector path from the root to this context location.
     *
     * @return a string representing the path.
     */
    @Override
    public String toString() {
      if (selector == null) {
        return "";
      }
      var prefix = (parent == null) ? "" : parent.toString();

      if (selector instanceof String s) {
        if (!prefix.isEmpty()) {
          return "%s.%s".formatted(prefix, s);
        } else {
          return s;
        }
      } else {
        var n = (Integer) selector;
        if (!prefix.isEmpty()) {
          return "%s[%d]".formatted(prefix, n);
        } else {
          throw new IllegalArgumentException("Unexpected value: " + selector);
        }
      }
    }

    /**
     * Is there a cycle in the traversal path?
     *
     * @return true if there is a cycle.
     */
    public boolean isCycle() {
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
  public void validateSimpleJson(Object tree) {
    var scheduled = new ArrayDeque<SelectionPath>();
    scheduled.add(new SelectionPath(tree));

    while (!scheduled.isEmpty()) {
      var item = scheduled.pop();

      if (item.isCycle()) {
        throw new IllegalArgumentException("Cycle detected at " + item);
      }

      final var target = item.getTarget();

      if (
        target == null ||
        target instanceof String ||
        target instanceof Number ||
        target instanceof Boolean
      ) {
        // Valid scalar values.
        continue;
      }

      if (target instanceof Map<?, ?> map) {
        map.forEach((k, v) -> {
          if (k instanceof String s) {
            // Valid if all children are valid.
            scheduled.add(new SelectionPath(item, s, v));
          } else {
            throw new IllegalArgumentException("Unexpected key: " + k + " at " + item);
          }
        });
        continue;
      }

      if (target instanceof List<?> list) {
        for (int i = 0; i < list.size(); i++) {
          // Valid if all children are valid.
          scheduled.add(new SelectionPath(item, i, list.get(i)));
        }
        continue;
      }

      throw new IllegalArgumentException(
        "Unexpected value type (%s) at %s".formatted(target.getClass().getSimpleName(), item)
      );
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
  @Nullable @SuppressWarnings("InconsistentOverloads")
  public <T> T fromMsgPack(@Nonnull byte[] bytes, int offset, int len, @Nonnull Class<T> clazz) {
    try {
      return MSGPACK_MAPPER.readValue(bytes, offset, len, clazz);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
