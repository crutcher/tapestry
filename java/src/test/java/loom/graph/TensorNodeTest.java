package loom.graph;

import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class TensorNodeTest implements CommonAssertions {
  @Test
  public void test_json() {
    var node = TensorNode.builder().dtype("float32").shape(new ZPoint(2, 3)).build();

    assertJsonEquals(
        node,
        "{\"@type\":\"tensor\",\"id\":\"" + node.id + "\",\"dtype\":\"float32\",\"shape\":[2,3]}}");
  }
}
