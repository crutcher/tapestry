package org.tensortapestry.zspace.ops;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.ZTensor;

class ChunkOpsTest implements WithAssertions {

  @Test
  public void concat() {
    var a = ZTensor.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    var b = ZTensor.newMatrix(new int[][] { { 5, 6 }, { 7, 8 } });

    assertThat(ChunkOps.concat(0, a, b))
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } }));
    assertThat(ChunkOps.concat(1, a, b))
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 1, 2, 5, 6 }, { 3, 4, 7, 8 } }));
  }
}
