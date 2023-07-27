package loom.alt.objgraph;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.val;
import loom.common.JsonUtil;
import loom.testing.CommonAssertions;
import org.junit.Test;

import javax.json.Json;
import java.util.List;

public class FNodeTest implements CommonAssertions {

  @Test
  public void testFoo() {
    FNode actual =
        FNode.builder()
            .type(OGBuiltins.TENSOR)
            .attr(OGBuiltins.SHAPE, List.of(1, 2, 3))
            .attr(OGBuiltins.DTYPE, "float32")
            .build();

    System.out.println(actual.toPrettyJsonString());

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(
            () ->
                actual
                    .getAttrs()
                    .put(OGBuiltins.EXTERNAL, JsonNodeFactory.instance.booleanNode(true)));

    var expected =
        Json.createObjectBuilder()
            .add("id", actual.getId().toString())
            .add("type", OGBuiltins.TENSOR.toString())
            .add(
                "attrs",
                Json.createObjectBuilder()
                    .add(
                        OGBuiltins.SHAPE.toString(), Json.createArrayBuilder().add(1).add(2).add(3))
                    .add(OGBuiltins.DTYPE.toString(), Json.createValue("float32")))
            .build()
            .toString();

    assertJsonEquals(actual, expected);

    var bar = actual.toBuilder().attr(OGBuiltins.EXTERNAL, false).build();

    var barExpected =
        Json.createObjectBuilder()
            .add("id", actual.getId().toString())
            .add("type", OGBuiltins.TENSOR.toString())
            .add(
                "attrs",
                Json.createObjectBuilder()
                    .add(
                        OGBuiltins.SHAPE.toString(), Json.createArrayBuilder().add(1).add(2).add(3))
                    .add(OGBuiltins.DTYPE.toString(), Json.createValue("float32"))
                    .add(OGBuiltins.EXTERNAL.toString(), false))
            .build()
            .toString();

    assertJsonEquals(bar, barExpected);
  }

  @Test
  public void testNodes() {
    val tree =
        JsonUtil.readTree(
            Json.createObjectBuilder().add("foo", 12).add("bar", 3.0).build().toString());

    assertJsonEquals(tree, Json.createObjectBuilder().add("foo", 12).add("bar", 3.0));
  }
}
