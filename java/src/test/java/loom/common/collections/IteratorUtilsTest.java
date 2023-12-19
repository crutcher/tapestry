package loom.common.collections;

import java.util.ArrayList;
import java.util.List;
import loom.testing.BaseTestClass;
import org.junit.Test;

public class IteratorUtilsTest extends BaseTestClass {
  @Test
  public void testIterableToStream() {
    var items = List.of("a", "b", "c");
    var collecting = new ArrayList<String>();
    IteratorUtils.iterableToStream(items).forEach(collecting::add);
    assertThat(collecting).containsExactlyElementsOf(items);
  }

  @Test
  public void testSupplierToIterable() {
    var items = List.of("a", "b", "c");
    var collecting = new ArrayList<String>();
    for (var item : IteratorUtils.supplierToIterable(items::iterator)) {
      collecting.add(item);
    }
    assertThat(collecting).containsExactlyElementsOf(items);
  }

  @Test
  public void testIterableIsNotEmpty() {
    assertThat(IteratorUtils.iterableIsNotEmpty(List.of("a"))).isTrue();
    assertThat(IteratorUtils.iterableIsNotEmpty(List.of())).isFalse();
    assertThat(IteratorUtils.iterableIsNotEmpty(null)).isFalse();
  }
}
