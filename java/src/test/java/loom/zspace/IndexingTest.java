package loom.zspace;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class IndexingTest implements CommonAssertions {

  @Test
  public void test_iota() {
    assertThat(Indexing.iota(0)).isEqualTo(new int[] {});
    assertThat(Indexing.iota(3)).isEqualTo(new int[] {0, 1, 2});
  }

  @Test
  public void test_aoti() {
    assertThat(Indexing.aoti(0)).isEqualTo(new int[] {});
    assertThat(Indexing.aoti(3)).isEqualTo(new int[] {2, 1, 0});
  }

  @Test
  public void test_resolveIndex() {
    assertThat(Indexing.resolveIndex("test", 0, 3)).isEqualTo(0);
    assertThat(Indexing.resolveIndex("test", 2, 3)).isEqualTo(2);
    assertThat(Indexing.resolveIndex("test", -1, 3)).isEqualTo(2);
    assertThat(Indexing.resolveIndex("test", -3, 3)).isEqualTo(0);

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolveIndex("test", 3, 3))
        .withMessageContaining("test: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolveIndex("test", -4, 3))
        .withMessageContaining("test: index -4 out of range [0, 3)");
  }

  @Test
  public void test_resolveDim() {
    assertThat(Indexing.resolveDim(0, 3)).isEqualTo(0);
    assertThat(Indexing.resolveDim(2, 3)).isEqualTo(2);
    assertThat(Indexing.resolveDim(-1, 3)).isEqualTo(2);
    assertThat(Indexing.resolveDim(-3, 3)).isEqualTo(0);

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolveDim(3, 3))
        .withMessageContaining("invalid dimension: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolveDim(-4, 3))
        .withMessageContaining("invalid dimension: index -4 out of range [0, 3)");

    var shape = new int[3];

    assertThat(Indexing.resolveDim(0, shape)).isEqualTo(0);
    assertThat(Indexing.resolveDim(2, shape)).isEqualTo(2);
    assertThat(Indexing.resolveDim(-1, shape)).isEqualTo(2);
    assertThat(Indexing.resolveDim(-3, shape)).isEqualTo(0);

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolveDim(3, shape))
        .withMessageContaining("invalid dimension: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolveDim(-4, shape))
        .withMessageContaining("invalid dimension: index -4 out of range [0, 3)");
  }

  @Test
  public void test_resolvePermutation() {
    assertThat(Indexing.resolvePermutation(new int[] {0, 1, 2}, 3)).isEqualTo(new int[] {0, 1, 2});
    assertThat(Indexing.resolvePermutation(new int[] {2, 1, 0}, 3)).isEqualTo(new int[] {2, 1, 0});
    assertThat(Indexing.resolvePermutation(new int[] {-1, -2, -3}, 3))
        .isEqualTo(new int[] {2, 1, 0});
    assertThat(Indexing.resolvePermutation(new int[] {-3, -2, -1}, 3))
        .isEqualTo(new int[] {0, 1, 2});

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolvePermutation(new int[] {3, 4, 5}, 3))
        .withMessageContaining("invalid dimension: index 3 out of range [0, 3)");

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.resolvePermutation(new int[] {-4, -5, -6}, 3))
        .withMessageContaining("invalid dimension: index -4 out of range [0, 3)");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> Indexing.resolvePermutation(new int[] {1, 1, 2}, 3))
        .withMessageContaining("invalid permutation: [1, 1, 2]");
  }

  @Test
  public void test_ravel() {
    var shape = new int[] {2, 1, 3};
    var strides = new int[] {3, 0, 1};

    assertThat(Indexing.ravel(shape, strides, new int[] {0, 0, 0})).isEqualTo(0);
    assertThat(Indexing.ravel(shape, strides, new int[] {1, 0, 1})).isEqualTo(4);
    assertThat(Indexing.ravel(shape, strides, new int[] {1, 0, -1})).isEqualTo(5);
    assertThat(Indexing.ravel(shape, strides, new int[] {-1, -1, -1})).isEqualTo(5);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> Indexing.ravel(new int[] {2, 2}, strides, new int[] {0, 0, 0}));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> Indexing.ravel(shape, new int[] {2, 2}, new int[] {0, 0, 0}));

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> Indexing.ravel(shape, strides, new int[] {0, 1, 0}));
  }

  @Test
  public void test_shapeToLFSStrides() {
    assertThat(Indexing.shapeToLSFStrides(new int[] {2, 1, 3})).isEqualTo(new int[] {3, 0, 1});

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> Indexing.shapeToLSFStrides(new int[] {2, 1, -1, 3}));
  }

  @Test
  public void test_shapeToSize() {
    assertThat(Indexing.shapeToSize(new int[] {2, 1, 3})).isEqualTo(6);
    assertThat(Indexing.shapeToSize(new int[] {2, 1, 0, 3})).isEqualTo(0);
  }

  @Test
  public void test_commonBroadcastShape() {
    assertThat(Indexing.commonBroadcastShape(new int[] {2, 1, 1}, new int[] {3}))
        .isEqualTo(new int[] {2, 1, 3});
  }
}
