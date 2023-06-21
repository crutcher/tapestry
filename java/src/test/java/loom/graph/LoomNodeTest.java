package loom.graph;

import java.util.UUID;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class LoomNodeTest implements CommonAssertions {
  @Test
  public void test_json() {
    var node = LoomNode.builder().build();
    UUID dep = UUID.randomUUID();
    node.addDep("type", "name", dep);

    assertJsonEquals(
        node,
        "{\"@type\":\"node\",\"id\":\""
            + node.id
            + "\",\"deps\":{\"type\":{\"name\":\""
            + dep
            + "\"}}}");
  }
}
