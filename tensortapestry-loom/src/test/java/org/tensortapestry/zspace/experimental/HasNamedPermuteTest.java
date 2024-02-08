package org.tensortapestry.zspace.experimental;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.tensortapestry.zspace.HasDimension;
import org.tensortapestry.zspace.exceptions.ZDimMissMatchError;
import org.tensortapestry.zspace.indexing.IndexingFns;

public class HasNamedPermuteTest implements ZSpaceTestAssertions {

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
    public int indexOfName(@Nonnull String name) {
      for (int i = 0; i < names.length; i++) {
        if (names[i].equals(name)) {
          return i;
        }
      }
      throw new IndexOutOfBoundsException(name);
    }

    @Override
    @Nonnull
    public String nameOfIndex(int index) {
      return names[index];
    }

    @Override
    @Nonnull
    public Example permute(@Nonnull int... permutation) {
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
    assertThat(example.permute(0, 1, 2))
      .isEqualTo(example.permute(List.of(0, 1, 2)))
      .isEqualTo(example);
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

    assertThat(example.permuteByNames("c", "b", "a"))
      .isEqualTo(
        Example.builder().shape(new int[] { 3, 2, 1 }).names(new String[] { "c", "b", "a" }).build()
      );
    assertThat(example.permuteByNames(List.of("c", "b", "a")))
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
    assertThat(example.indexOfName("a")).isEqualTo(0);
    assertThat(example.indexOfName("b")).isEqualTo(1);
    assertThat(example.indexOfName("c")).isEqualTo(2);
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> example.indexOfName("d"))
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
    assertThat(example.nameOfIndex(0)).isEqualTo("a");
    assertThat(example.nameOfIndex(1)).isEqualTo("b");
    assertThat(example.nameOfIndex(2)).isEqualTo("c");
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> example.nameOfIndex(3))
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
