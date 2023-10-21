package loom.common.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.testing.BaseTestClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapValueListUtilTest extends BaseTestClass {
  @Data
  public static class TestDoc {
    @Data
    @Jacksonized
    @SuperBuilder
    public static class Node {
      private final UUID id;
    }

    @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
    @JsonDeserialize(using = NodeListToMapDeserializer.class)
    private final Map<UUID, Node> nodes = new HashMap<>();

    public static class NodeListToMapDeserializer
        extends MapValueListUtil.MapDeserializer<UUID, Node> {
      public NodeListToMapDeserializer() {
        super(Node.class, Node::getId, HashMap.class);
      }
    }
  }

  @Test
  public void testBasic() throws Exception {
    String json =
        """
                {
                  "nodes": [
                     {
                       "id": "00000000-0000-0000-0000-000000000000"
                     }
                  ]
                }
                """;

    var doc = new TestDoc();
    var node =
        TestDoc.Node.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000000")).build();
    doc.nodes.put(node.getId(), node);

    assertJsonEquals(doc, json);
  }

  @Test
  public void testTypes() throws Exception {
    var deserializer = new TestDoc.NodeListToMapDeserializer();

    assertThat(deserializer.arrayType()).isEqualTo(TestDoc.Node[].class);
    assertThat(deserializer.newMap()).isInstanceOf(HashMap.class);
  }
}
