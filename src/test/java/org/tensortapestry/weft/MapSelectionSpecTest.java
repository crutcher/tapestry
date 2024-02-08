package org.tensortapestry.weft;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class MapSelectionSpecTest implements CommonAssertions {

  @Test
  public void test_SelectionSpec_nonexhaustive() {
    var spec = MapSelectionSpec
      .builder()
      .item("three", 3)
      .nonEmptyList("nonempty")
      .emptyList("empty")
      .exhaustive(false)
      .build();

    assertThat(
      spec.selectAsList(
        Map.of(
          "nonempty",
          List.of(13, 14),
          "empty",
          List.of(),
          "three",
          List.of(15, 16, 17),
          "ignored",
          List.of(18)
        )
      )
    )
      .isEqualTo(List.of(List.of(15, 16, 17), List.of(13, 14), List.of()));

    assertThat(
      spec.selectAsMap(
        Map.of(
          "nonempty",
          List.of(13, 14),
          "empty",
          List.of(),
          "three",
          List.of(15, 16, 17),
          "ignored",
          List.of(18)
        )
      )
    )
      .isEqualTo(
        Map.of("three", List.of(15, 16, 17), "nonempty", List.of(13, 14), "empty", List.of())
      );
  }

  @Test
  public void test_SelectionSpec_exhaustive() {
    var spec = MapSelectionSpec
      .builder()
      .item("three", 3)
      .nonEmptyList("nonempty")
      .emptyList("empty")
      .build();

    assertThat(
      spec.selectAsList(
        Map.of("nonempty", List.of(13, 14), "empty", List.of(), "three", List.of(15, 16, 17))
      )
    )
      .isEqualTo(List.of(List.of(15, 16, 17), List.of(13, 14), List.of()));

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() ->
        spec.check(
          Map.of("nonempty", List.of(13, 14), "empty", List.of(), "three", List.of(15, 16, 17, 18))
        )
      )
      .withMessageContaining("Expected size 3 for \"three\", but found 4:");

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() ->
        spec.check(Map.of("nonempty", List.of(), "empty", List.of(9), "three", List.of(15, 16, 17)))
      )
      .withMessageContaining("Expected non-empty \"nonempty\":");

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> spec.check(Map.of("nonempty", List.of(13, 14), "empty", List.of())))
      .withMessageContaining("Missing required item \"three\":");

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() ->
        spec.check(
          Map.of(
            "nonempty",
            List.of(13, 14),
            "empty",
            List.of(9),
            "three",
            List.of(15, 16, 17),
            "four",
            List.of(18)
          )
        )
      )
      .withMessageContaining(
        "Expecting [three, nonempty, empty] exhaustive keys, but found extra: [four]"
      );
  }
}
