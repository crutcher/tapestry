package org.tensortapestry.loom.zspace;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.tensortapestry.loom.testing.BaseTestClass;
import org.junit.Test;

public class HasNamedPermuteTest extends BaseTestClass {

  @Value
  @Builder
  public static class Example implements HasNamedPermute<Example> {

    int[] shape;
    String[] names;

    @Override
    public int getNDim() {
      return shape.length;
    }

    @Override
    public int indexOf(String name) {
      for (int i = 0; i < names.length; i++) {
        if (names[i].equals(name)) {
          return i;
        }
      }
      throw new IndexOutOfBoundsException(name);
    }

    @Override
    @Nonnull
    public String nameOf(int index) {
      return names[index];
    }

    @Override
    public Example permute(int... permutation) {
      var perm = IndexingFns.resolvePermutation(permutation, getNDim());
      return Example
        .builder()
        .shape(IndexingFns.applyResolvedPermutation(shape, perm))
        .names(IndexingFns.applyResolvedPermutation(names, perm))
        .build();
    }
  }

  @Test
  public void test_assertNDim() {
    var example = Example
      .builder()
      .shape(new int[] { 1, 2, 3 })
      .names(new String[] { "a", "b", "c" })
      .build();
    example.assertNDim(3);
    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> example.assertNDim(2))
      .withMessageContaining("Expected ndim 2, got 3");
  }

  @Test
  public void test_assertSameNDim() {
    var example1 = Example
      .builder()
      .shape(new int[] { 1, 2, 3 })
      .names(new String[] { "a", "b", "c" })
      .build();
    var example2 = Example
      .builder()
      .shape(new int[] { 4, 5, 6 })
      .names(new String[] { "x", "y", "z" })
      .build();
    var example3 = Example
      .builder()
      .shape(new int[] { 1, 2, 3, 4 })
      .names(new String[] { "a", "b", "c", "d" })
      .build();

    HasDimension.assertSameNDim(example1, example1);
    HasDimension.assertSameNDim(example1, example2);

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> HasDimension.assertSameNDim(example1, example3))
      .withMessageContaining("ZDim mismatch: [3, 4]");
  }

  @Test
  public void test_getNDim() {
    var example = Example
      .builder()
      .shape(new int[] { 1, 2, 3 })
      .names(new String[] { "a", "b", "c" })
      .build();
    assertThat(example.getNDim()).isEqualTo(3);
    assertThat(example.isScalar()).isFalse();

    var scalar = Example.builder().shape(new int[] {}).names(new String[] {}).build();
    assertThat(scalar.getNDim()).isEqualTo(0);
    assertThat(scalar.isScalar()).isTrue();
  }

  @Test
  public void test_permute() {
    var example = Example
      .builder()
      .shape(new int[] { 1, 2, 3 })
      .names(new String[] { "a", "b", "c" })
      .build();
    assertThat(example.permute(0, 1, 2)).isEqualTo(example);
    assertThat(example.permute(0, 2, 1))
      .isEqualTo(
        Example.builder().shape(new int[] { 1, 3, 2 }).names(new String[] { "a", "c", "b" }).build()
      );
    assertThat(example.permute(1, 0, 2))
      .isEqualTo(
        Example.builder().shape(new int[] { 2, 1, 3 }).names(new String[] { "b", "a", "c" }).build()
      );
    assertThat(example.permute(1, 2, 0))
      .isEqualTo(
        Example.builder().shape(new int[] { 2, 3, 1 }).names(new String[] { "b", "c", "a" }).build()
      );
    assertThat(example.permute(2, 0, 1))
      .isEqualTo(
        Example.builder().shape(new int[] { 3, 1, 2 }).names(new String[] { "c", "a", "b" }).build()
      );
    assertThat(example.permute(2, 1, 0))
      .isEqualTo(
        Example.builder().shape(new int[] { 3, 2, 1 }).names(new String[] { "c", "b", "a" }).build()
      );

    assertThat(example.permute("c", "b", "a"))
      .isEqualTo(
        Example.builder().shape(new int[] { 3, 2, 1 }).names(new String[] { "c", "b", "a" }).build()
      );
    assertThat(example.permute(List.of("c", "b", "a")))
      .isEqualTo(
        Example.builder().shape(new int[] { 3, 2, 1 }).names(new String[] { "c", "b", "a" }).build()
      );
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void test_indexOf() {
    var example = Example
      .builder()
      .shape(new int[] { 1, 2, 3 })
      .names(new String[] { "a", "b", "c" })
      .build();
    assertThat(example.indexOf("a")).isEqualTo(0);
    assertThat(example.indexOf("b")).isEqualTo(1);
    assertThat(example.indexOf("c")).isEqualTo(2);
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> example.indexOf("d"))
      .withMessageContaining("d");
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void test_nameOf() {
    var example = Example
      .builder()
      .shape(new int[] { 1, 2, 3 })
      .names(new String[] { "a", "b", "c" })
      .build();
    assertThat(example.nameOf(0)).isEqualTo("a");
    assertThat(example.nameOf(1)).isEqualTo("b");
    assertThat(example.nameOf(2)).isEqualTo("c");
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> example.nameOf(3))
      .withMessageContaining("3");
  }

  @Test
  public void test_toPermutation() {
    var example = Example
      .builder()
      .shape(new int[] { 1, 2, 3 })
      .names(new String[] { "a", "b", "c" })
      .build();
    assertThat(example.toPermutation("a", "b", "c")).isEqualTo(new int[] { 0, 1, 2 });
    assertThat(example.toPermutation("a", "c", "b")).isEqualTo(new int[] { 0, 2, 1 });
    assertThat(example.toPermutation("b", "a", "c")).isEqualTo(new int[] { 1, 0, 2 });
    assertThat(example.toPermutation("b", "c", "a")).isEqualTo(new int[] { 1, 2, 0 });
    assertThat(example.toPermutation("c", "a", "b")).isEqualTo(new int[] { 2, 0, 1 });
    assertThat(example.toPermutation("c", "b", "a")).isEqualTo(new int[] { 2, 1, 0 });

    assertThat(example.toPermutation(List.of("c", "b", "a"))).isEqualTo(new int[] { 2, 1, 0 });

    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> example.toPermutation("a", "b", "d"))
      .withMessageContaining("d");
  }
}