package org.tensortapestry.common.collections;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

import java.util.Set;

class CollectionContractsTest implements CommonAssertions {
  @Test
  void expectDistinct() {
    CollectionContracts.expectDistinct(Set.of(1, 2, 3), Set.of(4, 5, 6));


    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> CollectionContracts.expectDistinct(Set.of(1, 2, 3, 5), Set.of(3, 4, 5)))
      .withMessage("Overlapping items between \"lhs\" and \"rhs\": [3, 5]");
  }
}