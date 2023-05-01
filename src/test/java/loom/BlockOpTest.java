package loom;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class BlockOpTest implements CommonAssertions {

  @Test
  public void testGetId() {
    var ex = BlockOp.builder().id("linear").build();
    assertThat(ex.getId()).isEqualTo("linear");
  }

  @Test
  public void testJson() throws Exception {
    var ex = BlockOp.builder().id("linear").build();

    assertJsonEquals(ex, "{\"id\":\"linear\"}");
  }
}
