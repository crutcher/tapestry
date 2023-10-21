package loom.xyzzy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.serialization.MapValueListUtil;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class XGraphDocTest implements CommonAssertions {
  @Data
  public static class XGraphDoc {
    @Data
    @Jacksonized
    @SuperBuilder
    public static class Node {
      private final UUID id;
    }

    @Nullable private UUID id;

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

    var doc = new XGraphDoc();
    var node =
        XGraphDoc.Node.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
            .build();
    doc.nodes.put(node.getId(), node);

    assertJsonEquals(doc, json);
  }
}
