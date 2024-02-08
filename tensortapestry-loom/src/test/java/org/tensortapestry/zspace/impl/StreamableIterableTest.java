package org.tensortapestry.zspace.impl;

import java.util.List;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

public class StreamableIterableTest implements WithAssertions {

  @Test
  public void test() {
    var iterable = (StreamableIterable<String>) () -> List.of("a", "b", "c").iterator();

    assertThat(iterable.stream()).containsExactly("a", "b", "c");
    assertThat(iterable.toList()).containsExactly("a", "b", "c");
  }
}
