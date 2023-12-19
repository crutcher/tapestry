package loom.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import lombok.NoArgsConstructor;

/**
 * Utilities for serializing and deserializing a {@code Map<K, T>} as a json {@code [T]}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Data
 * @Jacksonize
 * public static class TestDoc {
 *   @Data
 *   @Jacksonized
 *   @SuperBuilder
 *   public static class Node {
 *     private final UUID id;
 *   }
 *
 *   @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
 *   @JsonDeserialize(using = NodeListToMapDeserializer.class)
 *   private final Map<UUID, Node> nodes;
 *
 *   private static class NodeListToMapDeserializer extends MapValueListUtil.MapDeserializer<UUID, Node> {
 *     public NodeListToMapDeserializer() {
 *       super(Node.class, Node::getId, HashMap.class);
 *     }
 *   }
 * }
 * }</pre>
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class MapValueListUtil {

  /** Serializer to write a {@code Map<K, T>} as a json {@code [T]}. */
  public static final class MapSerializer<K extends Comparable<K>, V>
      extends JsonSerializer<Map<K, V>> {
    @Override
    public void serialize(Map<K, V> value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeStartArray();

      // Stable output ordering.
      for (var entry : value.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
        gen.writeObject(entry.getValue());
      }

      gen.writeEndArray();
    }
  }

  /** Deserializer to read a json {@code [T]} as a {@code Map<K, T>}. */
  public static class MapDeserializer<K, V> extends JsonDeserializer<Map<K, V>> {
    private final Class<V> valueClass;
    private final Function<V, K> keyExtractor;

    private final Class<? extends Map<K, V>> mapClass;

    public MapDeserializer(
        Class<V> valueClass,
        Function<V, K> keyExtractor,
        @SuppressWarnings("rawtypes") Class<? extends Map> mapClass) {
      this.valueClass = valueClass;
      this.keyExtractor = keyExtractor;

      @SuppressWarnings("unchecked")
      var tmp = (Class<? extends Map<K, V>>) mapClass;
      this.mapClass = tmp;
    }

    Class<V[]> arrayType() {
      @SuppressWarnings("unchecked")
      var typ = (Class<V[]>) java.lang.reflect.Array.newInstance(valueClass, 0).getClass();
      return typ;
    }

    Map<K, V> newMap() {
      try {
        return mapClass.getDeclaredConstructor().newInstance();
      } catch (NoSuchMethodException
          | InvocationTargetException
          | InstantiationException
          | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Map<K, V> deserialize(
        com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
      var map = newMap();
      for (var v : p.readValueAs(arrayType())) {
        map.put(keyExtractor.apply(v), v);
      }
      return map;
    }
  }
}
