package loom.graph;

import java.util.UUID;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class TensorNodeTest implements CommonAssertions {
  @Test
  public void test_json() {
    var node = TensorNode.builder().dtype("float32").shape(new ZPoint(2, 3)).build();

    UUID dep = UUID.randomUUID();
    node.addDep("source", "generator", dep);

    assertJsonEquals(
        node,
        "{\"@type\":\"tensor\",\"id\":\""
            + node.id
            + "\",\"dtype\":\"float32\",\"shape\":[2,3],\"deps\":{\"source\":{\"generator\":\""
            + dep
            + "\"}}}");
  }
}
