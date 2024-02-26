package org.tensortapestry.zspace.indexing;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

class BufferOwnershipTest implements CommonAssertions {

  @Test
  public void test_apply() {
    var arr = new int[] { 1, 2, 3 };
    assertThat(BufferOwnership.REUSED.apply(arr)).isSameAs(arr);
    assertThat(BufferOwnership.CLONED.apply(arr))
      .isNotSameAs(arr)
      .usingComparator(Arrays::compare)
      .isEqualTo(arr);
  }
}
