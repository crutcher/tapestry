package loom.zspace;

import loom.testing.BaseTestClass;
import org.junit.Test;

public class GenericTensorTest extends BaseTestClass {
  @Test
  public void test_basic() {
    GenericTensor<Double> t = GenericTensor.newScalar(3.0);

    assertThat(t.get()).isEqualTo(3.0);

    assertThat(t.map(v -> v + 1.0).get()).isEqualTo(4.0);
  }
}
