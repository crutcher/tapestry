package org.tensortapestry.common.collections;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

class ListBuilderTest implements CommonAssertions {

  @SafeVarargs
  @SuppressWarnings("all")
  public static <T> List<T> listOfNullable(T... items) {
    var list = new ArrayList<T>();
    for (var item : items) {
      list.add(item);
    }
    return list;
  }

  @Test
  public void test() {
    var list = ListBuilder
      .<String>builder()
      .add("a")
      .add("b")
      .addNonNull(null)
      .addAll(List.of("c", "d"))
      .addAll("c", "d")
      .addAllNonNull(listOfNullable("e", null, "f"))
      .addAllNonNull("e", null, "f")
      .build();

    assertThat(list).containsExactly("a", "b", "c", "d", "c", "d", "e", "f", "e", "f");
  }
}
