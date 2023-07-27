package loom.common.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import loom.graph.LoomGraph;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Serializer to write a {@link Map < UUID ,  LoomGraph.Node >} as an array of {@link LoomGraph.Node}.
 */
public final class MapValueListSerializer<K extends Comparable<K>, V>
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
