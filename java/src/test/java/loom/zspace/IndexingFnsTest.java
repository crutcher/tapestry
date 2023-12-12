package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class IndexingFnsTest implements CommonAssertions {

  @Test
  public void test_iota() {
    assertThat(IndexingFns.iota(0)).isEqualTo(new int[] {});
    assertThat(IndexingFns.iota(3)).isEqualTo(new int[] {0, 1, 2});
  }

  @Test
  public void test_aoti() {
    assertThat(IndexingFns.aoti(0)).isEqualTo(new int[] {});
    assertThat(IndexingFns.aoti(3)).isEqualTo(new int[] {2, 1, 0});
  }

  @Test
  public void test_resolveIndex() {
    assertThat(IndexingFns.resolveIndex("test", 0, 3)).isEqualTo(0);
    assertThat(IndexingFns.resolveIndex("test", 2, 3)).isEqualTo(2);
    assertThat(IndexingFns.resolveIndex("test", -1, 3)).isEqualTo(2);
    assertThat(IndexingFns.resolveIndex("test", -3, 3)).isEqualTo(0);

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolveIndex("test", 3, 3))
        .withMessageContaining("test: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolveIndex("test", -4, 3))
        .withMessageContaining("test: index -4 out of range [0, 3)");
  }

  @Test
  public void test_resolveDim() {
    assertThat(IndexingFns.resolveDim(0, 3)).isEqualTo(0);
    assertThat(IndexingFns.resolveDim(2, 3)).isEqualTo(2);
    assertThat(IndexingFns.resolveDim(-1, 3)).isEqualTo(2);
    assertThat(IndexingFns.resolveDim(-3, 3)).isEqualTo(0);

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolveDim(3, 3))
        .withMessageContaining("invalid dimension: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolveDim(-4, 3))
        .withMessageContaining("invalid dimension: index -4 out of range [0, 3)");

    var shape = new int[3];

    assertThat(IndexingFns.resolveDim(0, shape)).isEqualTo(0);
    assertThat(IndexingFns.resolveDim(2, shape)).isEqualTo(2);
    assertThat(IndexingFns.resolveDim(-1, shape)).isEqualTo(2);
    assertThat(IndexingFns.resolveDim(-3, shape)).isEqualTo(0);

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolveDim(3, shape))
        .withMessageContaining("invalid dimension: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolveDim(-4, shape))
        .withMessageContaining("invalid dimension: index -4 out of range [0, 3)");
  }

  @Test
  public void test_resolvePermutation() {
    assertThat(IndexingFns.resolvePermutation(new int[] {0, 1, 2}, 3))
        .isEqualTo(new int[] {0, 1, 2});
    assertThat(IndexingFns.resolvePermutation(new int[] {2, 1, 0}, 3))
        .isEqualTo(new int[] {2, 1, 0});
    assertThat(IndexingFns.resolvePermutation(new int[] {-1, -2, -3}, 3))
        .isEqualTo(new int[] {2, 1, 0});
    assertThat(IndexingFns.resolvePermutation(new int[] {-3, -2, -1}, 3))
        .isEqualTo(new int[] {0, 1, 2});

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolvePermutation(new int[] {3, 4, 5}, 3))
        .withMessageContaining("invalid dimension: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.resolvePermutation(new int[] {-4, -5, -6}, 3))
        .withMessageContaining("invalid dimension: index -4 out of range [0, 3)");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IndexingFns.resolvePermutation(new int[] {1, 1, 2}, 3))
        .withMessageContaining("invalid permutation: [1, 1, 2]");
  }

  @Test
  public void test_ravel() {
    var shape = new int[] {2, 1, 3};
    var strides = new int[] {3, 0, 1};

    assertThat(IndexingFns.ravel(shape, strides, new int[] {0, 0, 0})).isEqualTo(0);
    assertThat(IndexingFns.ravel(shape, strides, new int[] {1, 0, 1})).isEqualTo(4);
    assertThat(IndexingFns.ravel(shape, strides, new int[] {1, 0, -1})).isEqualTo(5);
    assertThat(IndexingFns.ravel(shape, strides, new int[] {-1, -1, -1})).isEqualTo(5);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IndexingFns.ravel(new int[] {2, 2}, strides, new int[] {0, 0, 0}));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IndexingFns.ravel(shape, new int[] {2, 2}, new int[] {0, 0, 0}));

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IndexingFns.ravel(shape, strides, new int[] {0, 1, 0}));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IndexingFns.ravel(new int[] {2, 3}, new int[] {1}, new int[] {0, 0}))
        .withMessageContaining("shape [2, 3] and stride [1] must have the same dimensions");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IndexingFns.ravel(new int[] {2, 3}, new int[] {1, 2}, new int[] {0}))
        .withMessageContaining("shape [2, 3] and coords [0] must have the same dimensions");
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void test_shapeToLFSStrides() {
    assertThat(IndexingFns.shapeToLSFStrides(new int[] {2, 1, 3})).isEqualTo(new int[] {3, 0, 1});

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IndexingFns.shapeToLSFStrides(new int[] {2, 1, -1, 3}));
  }

  @Test
  public void test_shapeToSize() {
    assertThat(IndexingFns.shapeToSize(new int[] {2, 1, 3})).isEqualTo(6);
    assertThat(IndexingFns.shapeToSize(new int[] {2, 1, 0, 3})).isEqualTo(0);
  }

  @Test
  public void test_commonBroadcastShape() {
    assertThat(IndexingFns.commonBroadcastShape(new int[] {2, 1, 1}, new int[] {3}))
        .isEqualTo(new int[] {2, 1, 3});
  }
}
