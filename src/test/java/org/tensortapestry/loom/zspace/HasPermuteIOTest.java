package org.tensortapestry.loom.zspace;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Value;
import org.junit.Test;
import org.tensortapestry.loom.zspace.experimental.ZSpaceTestAssertions;

public class HasPermuteIOTest implements ZSpaceTestAssertions {

  @Value
  public static class Example implements HasPermuteIO<Example> {

    ZPoint input;
    ZPoint output;

    @Override
    public int getInputNDim() {
      return input.getNDim();
    }

    @Override
    public int getOutputNDim() {
      return output.getNDim();
    }

    @Override
    @Nonnull
    public Example permuteInput(@Nonnull int... permutation) {
      return new Example(input.permute(permutation), output);
    }

    @Override
    @Nonnull
    public Example permuteOutput(@Nonnull int... permutation) {
      return new Example(input, output.permute(permutation));
    }
  }

  @Test
  public void test() {
    var e = new Example(ZPoint.of(1, 2, 3), ZPoint.of(4, 5, 6, 7));

    assertThat(e.getInputNDim()).isEqualTo(3);
    assertThat(e.getOutputNDim()).isEqualTo(4);

    assertThat(e.resolveInputPermutation(-1, 0, 1)).isEqualTo(new int[] { 2, 0, 1 });

    assertThat(e.permuteInput(-1, 0, 1))
      .isEqualTo(e.permuteInput(List.of(-1, 0, 1)))
      .isEqualTo(new Example(ZPoint.of(3, 1, 2), ZPoint.of(4, 5, 6, 7)));

    assertThat(e.resolveOutputPermutation(-1, 0, -2, 1)).isEqualTo(new int[] { 3, 0, 2, 1 });

    assertThat(e.permuteOutput(-1, 0, -2, 1))
      .isEqualTo(e.permuteOutput(List.of(-1, 0, -2, 1)))
      .isEqualTo(new Example(ZPoint.of(1, 2, 3), ZPoint.of(7, 4, 6, 5)));
  }
}
