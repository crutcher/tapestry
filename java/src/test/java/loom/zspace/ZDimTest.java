package loom.zspace;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class ZDimTest implements WithAssertions {
  @Builder
  @Data
  @ToString
  static class ZDimImpl implements ZDim {
    int ndim;

    @Override
    public int ndim() {
      return ndim;
    }
  }

  @Test
  public void testAssertSameZDim() throws ZDimMissMatchError {
    ZDim.assertSameZDim(new ZDimImpl(1));
    ZDim.assertSameZDim(new ZDimImpl(1), new ZDimImpl(1));

    assertThatThrownBy(() -> ZDim.assertSameZDim(new ZDimImpl(1), new ZDimImpl(2)))
        .isInstanceOf(ZDimMissMatchError.class)
        .hasMessageContaining("ZDim mismatch: [1, 2]");
  }
}
