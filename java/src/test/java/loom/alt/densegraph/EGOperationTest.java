package loom.alt.densegraph;

import java.util.List;
import java.util.UUID;
import javax.json.Json;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class EGOperationTest implements CommonAssertions {
  @Test
  public void testJsonSimple() {
    var opId = UUID.randomUUID();
    var sigId = UUID.randomUUID();
    var op = EGOperation.builder().id(opId).signature(sigId).build();

    assertJsonEquals(
        op,
        Json.createObjectBuilder()
            .add("@type", "Operation")
            .add("id", opId.toString())
            .add("signature", sigId.toString()));
  }

  @Test
  public void testJson() {
    var opId = UUID.randomUUID();
    var sigId = UUID.randomUUID();
    var a = UUID.randomUUID();
    var b = UUID.randomUUID();

    var op =
        EGOperation.builder()
            .id(opId)
            .signature(sigId)
            .option("foo", "1")
            .input("a", List.of(a))
            .result("b", List.of(b))
            .build();

    assertJsonEquals(
        op,
        Json.createObjectBuilder()
            .add("@type", "Operation")
            .add("id", opId.toString())
            .add("signature", sigId.toString())
            .add(
                "inputs",
                Json.createObjectBuilder().add("a", Json.createArrayBuilder().add(a.toString())))
            .add(
                "results",
                Json.createObjectBuilder().add("b", Json.createArrayBuilder().add(b.toString())))
            .add("options", Json.createObjectBuilder().add("foo", "1")));

    // maps are immutable
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> op.getOptions().put("qux", "2"));

    assertJsonEquals(
        op.toBuilder().option("foo", "2").build(),
        Json.createObjectBuilder()
            .add("@type", "Operation")
            .add("id", opId.toString())
            .add("signature", sigId.toString())
            .add(
                "inputs",
                Json.createObjectBuilder().add("a", Json.createArrayBuilder().add(a.toString())))
            .add(
                "results",
                Json.createObjectBuilder().add("b", Json.createArrayBuilder().add(b.toString())))
            .add("options", Json.createObjectBuilder().add("foo", "2")));
  }
}
