package loom.graph.nodes;

import java.util.Map;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class GenericNodeTest extends BaseTestClass {

  @Test
  public void test_body() {
    var body = GenericNode.Body.builder().field("foo", "bar").field("abc", 99).build();

    body.setField("abc", 12);

    assertThat(body.getField("abc")).isEqualTo(12);

    assertThat(body.getFields()).isEqualTo(Map.of("foo", "bar", "abc", 12));

    body.setFields(Map.of("foo", "bar", "abc", 99));
    assertThat(body.getFields()).isEqualTo(Map.of("foo", "bar", "abc", 99));
  }
}
