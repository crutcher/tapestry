package loom.alt.objgraph;

import java.util.Map;
import javax.json.Json;
import loom.testing.CommonAssertions;
import loom.zspace.ZPoint;
import org.junit.Test;

public class OGNodeTest implements CommonAssertions {

  @Test
  public void testBuilder() {
    var node =
        OGNode.builder()
            .type(OGBuiltins.TENSOR)
            .attr(OGBuiltins.SHAPE, new ZPoint(2, 3))
            .attr(OGBuiltins.DTYPE, "int32")
            .build();

    assertJsonEquals(
        node,
        Json.createObjectBuilder()
            .add("id", node.getId().toString())
            .add("type", OGBuiltins.TENSOR.toString())
            .add(
                "attrs",
                Json.createObjectBuilder()
                    .add(OGBuiltins.SHAPE.toString(), "[2,3]")
                    .add(OGBuiltins.DTYPE.toString(), "\"int32\"")));
  }

  @Test
  public void testAttrsByNamespace() {
    String LOCAL_URN = "file:///tmp/foo";
    var node =
        OGNode.builder()
            .type(OGBuiltins.TENSOR)
            .attr(OGBuiltins.SHAPE, new ZPoint(2, 3))
            .attr(OGBuiltins.DTYPE, "int32")
            .attr(new JNSName(LOCAL_URN, "bar"), "baz")
            .build();

    Map<String, Map<String, String>> actual = node.attrsByNamespace();
    assertThat(actual)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                OGBuiltins.TENSOR.urn(),
                Map.of(
                    OGBuiltins.SHAPE.name(), "[2,3]",
                    OGBuiltins.DTYPE.name(), "\"int32\""),
                LOCAL_URN,
                Map.of("bar", "\"baz\"")));
  }
}
