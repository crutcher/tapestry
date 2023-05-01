package loom.matrix;

import loom.BlockOp;
import loom.testing.CommonAssertions;
import loom.zspace.ZRange;
import org.junit.Test;

public class BlockExpressionTest implements CommonAssertions {
  @Test
  public void basicConstructor() {
    var op = BlockOp.builder().id("op1").build();
    var index = BlockIndex.builder().range(ZRange.scalar()).build();
    var expr = BlockExpression.builder().op(op).index(index).build();

    assertThat(expr)
        .hasToString(
            "BlockExpression(op=BlockOp(id=op1), index=BlockIndex(range=ZRange(start=<[]>, end=<[]>)))");

    assertJsonEquals(
        expr, "{\"op\":{\"id\":\"op1\"},\"index\":{\"range\":{\"start\":[],\"end\":[]}}}");
  }
}
