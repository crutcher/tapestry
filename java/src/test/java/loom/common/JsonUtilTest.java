package loom.common;

import java.util.Map;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class JsonUtilTest implements CommonAssertions {
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
  @SuppressWarnings("unused")
  public void toMap() {
    var obj =
        new Object() {
          public final String a = "a";
          public final int b = 1;

          public final Object c =
              new Object() {
                public final String d = "d";
                public final int e = 2;
              };
        };

    Map<String, Object> res = JsonUtil.toMap(obj);
    assertThat(res)
        .hasEntrySatisfying("a", v -> assertThat(v).isEqualTo("a"))
        .hasEntrySatisfying("b", v -> assertThat(v).isEqualTo(1))
        .hasEntrySatisfying(
            "c",
            v -> {
              @SuppressWarnings("unchecked")
              Map<String, Object> m = (Map<String, Object>) v;
              assertThat(m)
                  .hasEntrySatisfying("d", w -> assertThat(w).isEqualTo("d"))
                  .hasEntrySatisfying("e", w -> assertThat(w).isEqualTo(2));
            });
  }
}
