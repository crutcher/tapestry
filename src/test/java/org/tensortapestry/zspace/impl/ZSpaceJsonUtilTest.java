package org.tensortapestry.zspace.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.ZTensor;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public class ZSpaceJsonUtilTest implements ZSpaceTestAssertions {

  @Test
  public void test() {
    var tensor = ZTensor.newMatrix(new int[][] { { 2, 3 }, { 4, 5 } });

    String json = "[ [2, 3], [4, 5] ]";

    assertObjectJsonEquivalence(tensor, json);

    assertThatJson(ZSpaceJsonUtil.toJson(tensor)).isEqualTo(json);
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZSpaceJsonUtil.toJson(new Object()))
      .withCauseInstanceOf(JsonProcessingException.class);

    assertThat(ZSpaceJsonUtil.fromJson(json, ZTensor.class)).isEqualTo(tensor);
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZSpaceJsonUtil.fromJson("x[", Object.class))
      .withCauseInstanceOf(JsonProcessingException.class);

    assertThatJson(ZSpaceJsonUtil.toPrettyJson(tensor)).isEqualTo(json);
    assertThat(ZSpaceJsonUtil.toPrettyJson(tensor)).isEqualTo("[ [ 2, 3 ], [ 4, 5 ] ]");
  }

  @Test
  public void test_pretty() {
    assertThat(
      ZSpaceJsonUtil.toPrettyJson(
        Map.of("foo", List.of(1, 2, 3), "bar", Map.of("x", 12, "y", "abc"))
      )
    )
      .isEqualTo(
        """
                {
                  "bar" : {
                    "x" : 12,
                    "y" : "abc"
                  },
                  "foo" : [ 1, 2, 3 ]
                }"""
      );

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZSpaceJsonUtil.toPrettyJson(new Object()))
      .withCauseInstanceOf(JsonProcessingException.class);
  }
}
