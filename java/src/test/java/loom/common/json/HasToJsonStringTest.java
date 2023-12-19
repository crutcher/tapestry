package loom.common.json;

import static org.junit.Assert.*;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class HasToJsonStringTest extends BaseTestClass {
  @Jacksonized
  @Builder
  @Data
  public static class Example implements HasToJsonString {
    private final String name;
    private final int age;
  }

  @Test
  public void test_toJsonString() {
    var example = Example.builder().name("John").age(42).build();
    assertEquals("{\"name\":\"John\",\"age\":42}", example.toJsonString());
    assertThat(example.toPrettyJsonString())
        .isEqualTo(
            """
                {
                  "name" : "John",
                  "age" : 42
                }""");
  }
}
