package org.tensortapestry.common.json;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class HasToJsonStringTest implements CommonAssertions {

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
    assertThat("{\"name\":\"John\",\"age\":42}").isEqualTo(example.toJsonString());
    assertThat(example.toPrettyJsonString())
        .isEqualTo(
            """
                {
                  "name" : "John",
                  "age" : 42
                }"""
        );
  }
}
