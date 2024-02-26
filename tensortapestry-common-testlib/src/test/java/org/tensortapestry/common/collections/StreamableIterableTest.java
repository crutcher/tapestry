package org.tensortapestry.common.collections;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

class StreamableIterableTest implements CommonAssertions {

  @Test
  public void test_stream() {
    var items = List.of("a", "b", "c");
    StreamableIterable<String> si = items::iterator;

    assertThat(si).containsExactly("a", "b", "c");
    assertThat(si.stream()).containsExactly("a", "b", "c");
    assertThat(si.toList()).containsExactly("a", "b", "c");
  }
}
