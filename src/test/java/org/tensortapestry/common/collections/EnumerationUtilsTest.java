package org.tensortapestry.common.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class EnumerationUtilsTest implements CommonAssertions {

  @Test
  public void testEnumerate() {
    var items = List.of("a", "b", "c");

    assertThat(EnumerationUtils.enumerate(items))
      .containsExactly(Map.entry(0, "a"), Map.entry(1, "b"), Map.entry(2, "c"));

    assertThat(EnumerationUtils.enumerate(items, -10))
      .containsExactly(Map.entry(-10, "a"), Map.entry(-9, "b"), Map.entry(-8, "c"));

    assertThat(EnumerationUtils.enumerate(items).withOffset(-10))
      .containsExactly(Map.entry(-10, "a"), Map.entry(-9, "b"), Map.entry(-8, "c"));
  }

  @Test
  public void test_EnumeratedIterable() {
    var items = List.of("a", "b", "c");
    var e = new EnumerationUtils.EnumeratedIterable<>(items);

    assertThat(e.getIterable()).isEqualTo(items);
    assertThat(e.getOffset()).isEqualTo(0);

    var e2 = e.withOffset(-10);
    assertThat(e2).isNotSameAs(e);
    assertThat(e2.getIterable()).isEqualTo(items);
    assertThat(e2.getOffset()).isEqualTo(-10);

    assertThat(e2).containsExactly(Map.entry(-10, "a"), Map.entry(-9, "b"), Map.entry(-8, "c"));
  }

  @Test
  public void test_EnumeratedIterator_no_offset() {
    var items = List.of("a", "b", "c");
    var e = new EnumerationUtils.EnumeratedIterator<>(items.iterator());

    assertThat(e.getOffset()).isEqualTo(0);

    var collecting = new ArrayList<Map.Entry<Integer, String>>();
    e.forEachRemaining(collecting::add);

    assertThat(collecting).containsExactly(Map.entry(0, "a"), Map.entry(1, "b"), Map.entry(2, "c"));
  }

  @Test
  public void test_EnumeratedIterator_offset() {
    var items = List.of("a", "b", "c");
    var e = new EnumerationUtils.EnumeratedIterator<>(items.iterator(), -10);

    assertThat(e.getOffset()).isEqualTo(-10);

    var collecting = new ArrayList<Map.Entry<Integer, String>>();
    e.forEachRemaining(collecting::add);

    assertThat(collecting)
      .containsExactly(Map.entry(-10, "a"), Map.entry(-9, "b"), Map.entry(-8, "c"));
  }
}
