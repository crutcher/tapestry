package org.tensortapestry.weft;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class SelectionMapUtilsTest implements CommonAssertions {

  @Test
  public void test_getSingularItem() {
    assertThat(SelectionMapUtils.getSingularItem(Map.of("foo", List.of(12)))).isEqualTo(12);

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() ->
        SelectionMapUtils.getSingularItem(Map.of("foo", List.of(12), "bar", List.of(13)))
      )
      .withMessage("Expected map with single entry, but found 2 entries: {bar=[13], foo=[12]}");

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> SelectionMapUtils.getSingularItem(Map.of("foo", List.of(12, 13))))
      .withMessage("Expected map with single entry, but found \"foo\" with 2 values: [12, 13]");
  }
}
