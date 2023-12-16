package loom.graph.nodes;

import loom.testing.BaseTestClass;
import org.junit.Test;

public class TensorNodeTest extends BaseTestClass {
  @Test
  public void testMeta() {
    var meta = TensorNode.Prototype.builder().validDType("int32").build();

    meta.addValidDType("float32");

    assertThat(meta.getValidDTypes()).contains("int32", "float32");
  }
}
