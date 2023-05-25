package loom.zspace;

import static org.junit.Assert.*;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class UtilityTest implements CommonAssertions {

  @Test
  public void iota() {
    assertThat(Utility.iota(0)).isEqualTo(new int[] {});
    assertThat(Utility.iota(3)).isEqualTo(new int[] {0, 1, 2});
  }

  @Test
  public void aoti() {
    assertThat(Utility.aoti(0)).isEqualTo(new int[] {});
    assertThat(Utility.aoti(3)).isEqualTo(new int[] {2, 1, 0});
  }
}
