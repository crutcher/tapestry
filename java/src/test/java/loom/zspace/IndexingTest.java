package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class IndexingTest implements CommonAssertions {

  @Test
  public void iota() {
    assertThat(Indexing.iota(0)).isEqualTo(new int[] {});
    assertThat(Indexing.iota(3)).isEqualTo(new int[] {0, 1, 2});
  }

  @Test
  public void aoti() {
    assertThat(Indexing.aoti(0)).isEqualTo(new int[] {});
    assertThat(Indexing.aoti(3)).isEqualTo(new int[] {2, 1, 0});
  }
}
