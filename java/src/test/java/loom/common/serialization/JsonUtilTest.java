package loom.common.serialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Value;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class JsonUtilTest implements CommonAssertions {
  @Value
  public static class Example {
    public String a;
    public int b;
  }

  @Test
  @SuppressWarnings("unused")
  public void toJsonString() {
    var obj =
        new Object() {
          public final String a = "a";
          public final int b = 1;
        };
    assertThat(JsonUtil.toJson(obj)).isEqualTo("{\"a\":\"a\",\"b\":1}");
    assertThat(JsonUtil.toPrettyJson(obj))
        .isEqualTo(
            """
                                {
                                  "a" : "a",
                                  "b" : 1
                                }""");
  }

  @Test
  public void testToSimpleJson() {
    var example = new Example("hello", 3);

    assertThat(JsonUtil.toSimpleJson(List.of(example)))
        .isEqualTo(List.of(Map.of("a", "hello", "b", 3)));
  }

  @Test
  public void testValidateSimpleJson() {
    JsonUtil.validateSimpleJson(2);

    JsonUtil.validateSimpleJson(2.0);
    JsonUtil.validateSimpleJson(Float.POSITIVE_INFINITY);
    JsonUtil.validateSimpleJson(Float.NEGATIVE_INFINITY);
    JsonUtil.validateSimpleJson(Float.NaN);

    JsonUtil.validateSimpleJson(true);
    JsonUtil.validateSimpleJson(false);
    JsonUtil.validateSimpleJson(null);

    JsonUtil.validateSimpleJson("hello");

    JsonUtil.validateSimpleJson(List.of(1, 2.0, "hello", true, false, Float.POSITIVE_INFINITY));

    JsonUtil.validateSimpleJson(
        Map.of(
            "a",
            1,
            "b",
            2.0,
            "c",
            "hello",
            "d",
            true,
            "e",
            false,
            "f",
            Float.POSITIVE_INFINITY,
            "g",
            List.of(1, 2.0, "hello", true, false, Float.POSITIVE_INFINITY),
            "h",
            Map.of("a", 1, "b", 2.0)));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.validateSimpleJson(new Object()));

    var cycle = new ArrayList<>();
    cycle.add(cycle);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> JsonUtil.validateSimpleJson(Map.of("foo", cycle)));
  }
}
