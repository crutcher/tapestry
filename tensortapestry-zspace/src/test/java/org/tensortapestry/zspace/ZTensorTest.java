package org.tensortapestry.zspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.zspace.exceptions.ZDimMissMatchError;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;
import org.tensortapestry.zspace.indexing.*;
import org.tensortapestry.zspace.ops.CellWiseOps;

public class ZTensorTest implements ZSpaceTestAssertions {

  @Test
  public void concat() {
    var a = ZTensor.newMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
    var b = ZTensor.newMatrix(new int[][] { { 5, 6 }, { 7, 8 } });

    assertThat(ZTensor.concat(0, a, b))
      .isEqualTo(ZTensor.concat(0, List.of(a, b)))
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } }));
    assertThat(ZTensor.concat(1, a, b))
      .isEqualTo(ZTensor.concat(1, List.of(a, b)))
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 1, 2, 5, 6 }, { 3, 4, 7, 8 } }));
  }

  @Test
  public void test_select() {
    var gen = new Random();

    var t = ZTensor.newFilled(new int[] { 2, 3, 4, 5 }, (IntSupplier) gen::nextInt);

    assertThat(t.select("1, +100, ..., +, :, ::-2"))
      .isEqualTo(
        t.select(
          Selector.index(1),
          Selector.newAxis(100),
          Selector.ellipsis(),
          Selector.newAxis(),
          Selector.sliceBuilder().build(),
          Selector.sliceBuilder().step(-2).build()
        )
      )
      .extracting(ZTensor::shapeAsList)
      .isEqualTo(List.of(100, 3, 1, 4, 3));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> t.select("..., ..."))
      .withMessage("Multiple ellipsis in selection: [..., ...]");

    assertThatExceptionOfType(NullPointerException.class)
      .isThrownBy(() -> t.select(null, null))
      .withMessage("selector");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> t.select(new BrokenSelector()))
      .withMessageContaining("Unsupported selector");
  }

  @Test
  public void test_sliceDim() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3, 4 }, { 5, 6, 7 } });

    assertThat(t.sliceDim(1, new Selector.Slice()))
      .isSameAs(t.sliceDim(1, new Selector.Slice(0, 3, 1)))
      .isSameAs(t.sliceDim(1, new Selector.Slice(-3, null, 1)))
      .isSameAs(t);

    assertThat(t.sliceDim(1, new Selector.Slice(1, 3)))
      .isEqualTo(t.sliceDim(1, 1, 3))
      .isEqualTo(t.sliceDim(1, 1, 3, null))
      .isEqualTo(t.sliceDim(1, new Selector.Slice(1, null)))
      .isEqualTo(t.sliceDim(1, new Selector.Slice(1, 3, 1)))
      .isEqualTo(t.sliceDim(1, new Selector.Slice(-2, null)))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 3, 4 }, { 6, 7 } }));

    assertThat(ZTensor.newZeros(3).sliceDim(0, new Selector.Slice(-2, null, -1)))
      .isEqualTo(ZTensor.newFromArray(new int[] { 0, 0 }));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZTensor.newZeros(3).sliceDim(0, new Selector.Slice(null, null, 0)))
      .withMessage("slice step cannot be zero: ::0");
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZTensor.newZeros(3).sliceDim(0, new Selector.Slice(1, 0)))
      .withMessage("slice start (1) must be less than end (0) for positive step (1): 1:0");
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZTensor.newZeros(3).sliceDim(0, new Selector.Slice(0, 1, -1)))
      .withMessage("slice start (0) must be greater than end (1) for negative step (-1): 0:1:-1");
  }

  @Test
  public void test_allZero() {
    assertThat(ZTensor.newZeros(2, 2).allZero()).isTrue();
    assertThat(ZTensor.newOnes(2, 2).allZero()).isFalse();

    assertThat(ZTensor.newOnes(2, 0).allZero()).isTrue();

    assertThat(ZTensor.newScalar(0).allZero()).isTrue();
    assertThat(ZTensor.newScalar(1).allZero()).isFalse();
  }

  @Test
  public void test_json() {
    var z3 = ZTensor.newZeros(0, 0, 0);
    assertObjectJsonEquivalence(z3, "[[[]]]");

    // Degenerate tensors map to emtpy tensors.
    var deg = ZTensor.newZeros(0, 5);
    assertThat(JsonUtil.toJson(deg)).isEqualTo("[[]]");

    var t = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });
    var s = ZTensor.newScalar(3);

    assertObjectJsonEquivalence(t, "[[2,3],[4,5]]");
    assertObjectJsonEquivalence(s, "3");

    // As a field.
    assertObjectJsonEquivalence(new JsonExampleContainer(t), "{\"tensor\": [[2,3],[4,5]]}");
    assertObjectJsonEquivalence(new JsonExampleContainer(s), "{\"tensor\": 3}");
  }

  @Test
  public void test_msgpack() {
    {
      var tensor = ZTensor.newZeros(5, 3, 2);
      assertThat(JsonUtil.fromMsgPack(JsonUtil.toMsgPack(tensor), ZTensor.class)).isEqualTo(tensor);
    }
    {
      var tensor = ZTensor.newScalar(12);
      assertThat(JsonUtil.fromMsgPack(JsonUtil.toMsgPack(tensor), ZTensor.class)).isEqualTo(tensor);
    }
  }

  @Test
  public void test_tofrom_base64() {
    var tensor = ZTensor.newOnes(5, 3, 2);
    assertThat(ZTensor.newFromFlatBase64(tensor.toFlatBase64())).isEqualTo(tensor);
  }

  @Test
  public void test_allMatch_anyMatch() {
    {
      var t = ZTensor.newScalar(3);

      assertThat(t.allMatch(x -> x == 3)).isTrue();
      assertThat(t.allMatch(x -> x == 4)).isFalse();

      assertThat(t.anyMatch(x -> x == 3)).isTrue();
      assertThat(t.anyMatch(x -> x == 4)).isFalse();
    }

    {
      var t = ZTensor.newVector(1, 2, 3);
      assertThat(t.allMatch(x -> x == 3)).isFalse();
      assertThat(t.allMatch(x -> x > 0)).isTrue();

      assertThat(t.anyMatch(x -> x == 3)).isTrue();
      assertThat(t.anyMatch(x -> x == 4)).isFalse();
    }

    {
      var t = ZTensor.newVector();
      assertThat(t.allMatch(x -> x == 3)).isTrue();

      assertThat(t.anyMatch(x -> x == 3)).isFalse();
    }
  }

  @Test
  public void test_equals() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });

    assertThat(t)
      .isEqualTo(t)
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } }))
      .isEqualTo(new int[][] { { 2, 3 }, { 4, 5 } })
      .isNotEqualTo(new int[][] { { 2, 3 }, { 4 } })
      .isNotEqualTo(null)
      .isNotEqualTo("abc")
      .isNotEqualTo(ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 6 } }))
      .isNotEqualTo(ZTensor.newFromArray(new int[] { 2, 3 }));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZTensor.newFromArray(new Object()))
      .withMessage("Cannot convert object of type java.lang.Object to ZTensor");
  }

  @Test
  public void test_fromTree() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() ->
        ZTensor.newFromTree(
          new Object(),
          obj -> obj.getClass().isArray(),
          Array::getLength,
          Array::get,
          obj -> (int) obj,
          int[].class::cast
        )
      )
      .withMessage("Could not parse array from tree");
  }

  @Test
  public void test_newIota() {
    assertThat(ZTensor.newIota(0)).isEqualTo(ZTensor.newZeros(0));
    assertThat(ZTensor.newIota(3)).isEqualTo(ZTensor.newFromArray(new int[] { 0, 1, 2 }));
  }

  @Test
  public void test_byCoords() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });

    {
      // CoordsBufferMode.SHARED

      IterableCoordinates coords = t.byCoords(BufferOwnership.REUSED);
      assertThat(coords.getBufferOwnership()).isEqualTo(BufferOwnership.REUSED);

      {
        var it = coords.iterator();
        assertThat(it.getBufferOwnership()).isEqualTo(BufferOwnership.REUSED);

        assertThat(it.hasNext()).isTrue();
        var buf = it.next();
        assertThat(buf).isEqualTo(new int[] { 0, 0 });
        assertThat(it.next()).isSameAs(buf).isEqualTo(new int[] { 0, 1 });
        assertThat(it.next()).isSameAs(buf).isEqualTo(new int[] { 1, 0 });
        assertThat(it.next()).isSameAs(buf).isEqualTo(new int[] { 1, 1 });
        assertThat(it.hasNext()).isFalse();

        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(it::next);
      }

      // This is the weird shit, because the buffer is reused.
      assertThat(coords.stream().toList())
        .containsExactly(
          new int[] { 1, 1 },
          new int[] { 1, 1 },
          new int[] { 1, 1 },
          new int[] { 1, 1 }
        );
      assertThat(coords.stream().map(int[]::clone).toList())
        .containsExactly(
          new int[] { 0, 0 },
          new int[] { 0, 1 },
          new int[] { 1, 0 },
          new int[] { 1, 1 }
        );

      var items = new ArrayList<int[]>();
      coords.iterator().forEachRemaining(b -> items.add(b.clone()));
      assertThat(items)
        .containsExactly(
          new int[] { 0, 0 },
          new int[] { 0, 1 },
          new int[] { 1, 0 },
          new int[] { 1, 1 }
        );
    }

    {
      // CoordsBufferMode.SAFE

      IterableCoordinates coords = t.byCoords(BufferOwnership.CLONED);
      assertThat(coords.getBufferOwnership()).isEqualTo(BufferOwnership.CLONED);

      {
        var it = coords.iterator();
        assertThat(it.getBufferOwnership()).isEqualTo(BufferOwnership.CLONED);
      }

      assertThat(coords.stream().toList())
        .containsExactly(
          new int[] { 0, 0 },
          new int[] { 0, 1 },
          new int[] { 1, 0 },
          new int[] { 1, 1 }
        );

      var items = new ArrayList<int[]>();
      coords.iterator().forEachRemaining(items::add);
      assertThat(items)
        .containsExactly(
          new int[] { 0, 0 },
          new int[] { 0, 1 },
          new int[] { 1, 0 },
          new int[] { 1, 1 }
        );
    }

    // Empty tensor.
    assertThat(ZTensor.newVector().byCoords(BufferOwnership.CLONED).stream().toList()).isEmpty();

    // Scalar tensor.
    assertThat(ZTensor.newScalar(2).byCoords(BufferOwnership.CLONED).stream().toList())
      .containsExactly(new int[] {});
  }

  @Test
  public void test_isStrictlyPositive() {
    assertThat(ZTensor.newScalar(1).isStrictlyPositive()).isTrue();
    assertThat(ZTensor.newScalar(0).isStrictlyPositive()).isFalse();

    assertThat(ZTensor.newVector(1, 2, 3).isStrictlyPositive()).isTrue();
    assertThat(ZTensor.newVector(1, 0, 3).isStrictlyPositive()).isFalse();
    assertThat(ZTensor.newVector(1, -0, 3).isStrictlyPositive()).isFalse();
  }

  @Test
  public void test_isNonNegative() {
    assertThat(ZTensor.newScalar(1).isNonNegative()).isTrue();
    assertThat(ZTensor.newScalar(0).isNonNegative()).isTrue();
    assertThat(ZTensor.newScalar(-1).isNonNegative()).isFalse();

    assertThat(ZTensor.newVector(1, 2, 3).isNonNegative()).isTrue();
    assertThat(ZTensor.newVector(1, 0, 3).isNonNegative()).isTrue();
    assertThat(ZTensor.newVector(1, -0, 3).isNonNegative()).isTrue();
    assertThat(ZTensor.newVector(1, -1, 3).isNonNegative()).isFalse();
  }

  @Test
  public void test_assertShape() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });

    assertThat(t.shapeAsArray()).isEqualTo(new int[] { 2, 2 });
    assertThat(t.shapeAsTensor()).isEqualTo(ZTensor.newVector(2, 2));

    t.assertShape(2, 2);

    assertThatThrownBy(() -> t.assertShape(2, 2, 1, 2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("shape [2, 2] != expected shape [2, 2, 1, 2]");
  }

  @Test
  public void test_hashCode() {
    var t = ZTensor.newVector(1, 2, 3).asImmutable();
    assertThat(t).hasSameHashCodeAs(ZTensor.newVector(1, 2, 3).asImmutable());

    var x = ZTensor.newVector(3, 2, 1);
    var view = x.reverse(0);

    //noinspection ResultOfMethodCallIgnored
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(view::hashCode);

    var t2 = view.asImmutable();

    assertThat(t).hasSameHashCodeAs(t2);
  }

  @Test
  public void test_scalars() {
    var tensor = ZTensor.newScalar(3);

    assertThat(tensor.getNDim()).isEqualTo(0);
    assertThat(tensor.shapeAsArray()).isEqualTo(new int[] {});
    assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.newVector());

    assertThat(tensor)
      .hasToString("3")
      .isEqualTo(ZTensor.newFromArray(3))
      .extracting(ZTensor::toArray)
      .isEqualTo(3);

    assertThat(tensor.toT0()).isEqualTo(3);

    assertThat(tensor.add(ZTensor.newScalar(2))).extracting(ZTensor::item).isEqualTo(5);

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> ZTensor.newVector(2, 3).toT0());
  }

  @Test
  public void test_vectors() {
    var tensor = ZTensor.newVector(3, 7);

    assertThat(tensor.getNDim()).isEqualTo(1);
    assertThat(tensor.shapeAsArray()).isEqualTo(new int[] { 2 });
    assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.newVector(2));

    assertThat(tensor)
      .hasToString("[3, 7]")
      .isEqualTo(ZTensor.newFromArray(new int[] { 3, 7 }))
      .extracting(ZTensor::toArray)
      .isEqualTo(new int[] { 3, 7 });

    assertThat(tensor.toT1()).isEqualTo(new int[] { 3, 7 });

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> ZTensor.newScalar(3).toT1());
  }

  @Test
  public void test_matrices() {
    var tensor = ZTensor.newMatrix(new int[] { 3, 7 }, new int[] { 8, 9 });

    assertThat(tensor.getNDim()).isEqualTo(2);
    assertThat(tensor.shapeAsArray()).isEqualTo(new int[] { 2, 2 });
    assertThat(tensor.shapeAsTensor()).isEqualTo(ZTensor.newVector(2, 2));

    assertThat(tensor)
      .hasToString("[[3, 7], [8, 9]]")
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 3, 7 }, { 8, 9 } }))
      .extracting(ZTensor::toArray)
      .isEqualTo(new int[][] { { 3, 7 }, { 8, 9 } });

    assertThat(tensor.toT2()).isEqualTo(new int[][] { { 3, 7 }, { 8, 9 } });

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZTensor.newMatrix(new int[] { 3, 7 }, new int[] { 8, 9, 10 }))
      .withMessage("All rows must have the same length, found: [2, 3]");

    assertThatExceptionOfType(ZDimMissMatchError.class)
      .isThrownBy(() -> ZTensor.newScalar(3).toT2());
  }

  @Test
  public void test_create() {
    var t0 = ZTensor.newScalar(3);
    var t1 = ZTensor.newVector(2, 3, 4);
    var t2 = ZTensor.newMatrix(new int[] { 2, 3 }, new int[] { 4, 5 });

    assertThat(t0.getNDim()).isEqualTo(0);
    assertThat(t0.getSize()).isEqualTo(1);
    assertThat(t0.item()).isEqualTo(3);

    assertThat(t1.getNDim()).isEqualTo(1);
    assertThat(t1.getSize()).isEqualTo(3);
    assertThat(t1.get(1)).isEqualTo(3);

    assertThat(t2.getNDim()).isEqualTo(2);
    assertThat(t2.getSize()).isEqualTo(4);
    assertThat(t2.get(1, 0)).isEqualTo(4);
  }

  @Test
  public void test_clone() {
    assertThat(ZTensor.newVector(2, 3).clone()).isEqualTo(ZTensor.newVector(2, 3));

    var compactMutableSource = ZTensor.newMatrix(new int[][] { { 2, 3 }, { 4, 5 } });
    assertThat(compactMutableSource)
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 2, 3 }, { 4, 5 } }))
      .hasFieldOrPropertyWithValue("mutable", true)
      .hasFieldOrPropertyWithValue("compact", true);
    assertThat(compactMutableSource.clone())
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 2, 3 }, { 4, 5 } }))
      .hasFieldOrPropertyWithValue("mutable", true)
      .hasFieldOrPropertyWithValue("compact", true)
      .isNotSameAs(compactMutableSource);

    var nonCompactMutableSource = compactMutableSource.selectDim(0, 0);
    assertThat(nonCompactMutableSource)
      .isEqualTo(ZTensor.newVector(2, 3))
      .hasFieldOrPropertyWithValue("mutable", true)
      .hasFieldOrPropertyWithValue("compact", false);
    assertThat(nonCompactMutableSource.clone())
      .isEqualTo(ZTensor.newVector(2, 3))
      .hasFieldOrPropertyWithValue("mutable", true)
      .hasFieldOrPropertyWithValue("compact", true)
      .isNotSameAs(nonCompactMutableSource);

    var compactImmutableSource = compactMutableSource.asImmutable();
    assertThat(compactImmutableSource)
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 2, 3 }, { 4, 5 } }))
      .hasFieldOrPropertyWithValue("mutable", false)
      .hasFieldOrPropertyWithValue("compact", true);
    assertThat(compactImmutableSource.clone())
      .isEqualTo(ZTensor.newMatrix(new int[][] { { 2, 3 }, { 4, 5 } }))
      .hasFieldOrPropertyWithValue("mutable", false)
      .hasFieldOrPropertyWithValue("compact", true)
      .isSameAs(compactImmutableSource);

    var nonCompactImmutableSource = compactImmutableSource.selectDim(0, 0);
    assertThat(nonCompactImmutableSource)
      .isEqualTo(ZTensor.newVector(2, 3))
      .hasFieldOrPropertyWithValue("mutable", false)
      .hasFieldOrPropertyWithValue("compact", false);
    assertThat(nonCompactImmutableSource.clone())
      .isEqualTo(ZTensor.newVector(2, 3))
      .hasFieldOrPropertyWithValue("mutable", false)
      .hasFieldOrPropertyWithValue("compact", true)
      .isNotSameAs(nonCompactMutableSource);
  }

  @Test
  public void test_fromArray_toArray() {
    {
      assertThat(ZTensor.newFromArray(3)).isEqualTo(ZTensor.newScalar(3));
      assertThat(ZTensor.newFromArray(new int[][] {})).isEqualTo(ZTensor.newZeros(0));
      assertThat(ZTensor.newFromArray(new int[][] { {} })).isEqualTo(ZTensor.newZeros(0, 0));
    }
    {
      var t = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });

      assertThat(t.getNDim()).isEqualTo(2);
      assertThat(t.getSize()).isEqualTo(4);
      assertThat(t.get(1, 0)).isEqualTo(4);

      var t2 = t.add(2);

      assertThat(t2.toArray()).isEqualTo(new int[][] { { 4, 5 }, { 6, 7 } });
    }
  }

  @Test
  public void test_newFromList() {
    {
      assertThat(ZTensor.newFromList(7)).isEqualTo(ZTensor.newScalar(7));
      assertThat(ZTensor.newFromList(List.of())).isEqualTo(ZTensor.newZeros(0));
      assertThat(ZTensor.newFromList(List.of(List.of()))).isEqualTo(ZTensor.newZeros(0, 0));
    }
    {
      var t = ZTensor.newFromList(List.of(List.of(2, 3), List.of(4, 5)));

      assertThat(t.getNDim()).isEqualTo(2);
      assertThat(t.getSize()).isEqualTo(4);
      assertThat(t.get(1, 0)).isEqualTo(4);

      var t2 = t.add(2);

      assertThat(t2.toArray()).isEqualTo(new int[][] { { 4, 5 }, { 6, 7 } });
    }
  }

  @Test
  public void test_toString_parse() {
    var t = ZTensor.newMatrix(new int[][] { { 2, 3 }, { 4, 5 } });
    assertThat(t.toString()).isEqualTo("[[2, 3], [4, 5]]");

    assertThat(ZTensor.newScalar(3).toString()).isEqualTo("3");
    assertThat(ZTensor.newZeros(3, 0).toString()).isEqualTo("[[]]");

    assertThat(ZTensor.parse("3")).isEqualTo(ZTensor.newScalar(3));
    assertThat(ZTensor.parse("[[2, 3]]")).isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 3 } }));
    assertThat(ZTensor.parse("[[[]]]")).isEqualTo(ZTensor.newZeros(0, 0, 0));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> ZTensor.parse("[[2, "));
  }

  @Test
  public void test_zeros() {
    assertThat(ZTensor.newZeros(2, 1))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 0 }, { 0 } }));
    assertThat(ZTensor.newZerosLike(ZTensor.newOnes(2, 1))).isEqualTo(ZTensor.newZeros(2, 1));
  }

  @Test
  public void test_ones() {
    assertThat(ZTensor.newOnes(2, 1)).isEqualTo(ZTensor.newFromArray(new int[][] { { 1 }, { 1 } }));
    assertThat(ZTensor.newOnesLike(ZTensor.newZeros(2, 1))).isEqualTo(ZTensor.newOnes(2, 1));
  }

  @Test
  public void test_full() {
    assertThat(ZTensor.newFilled(new int[] { 2, 1 }, 9))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 9 }, { 9 } }));
    assertThat(ZTensor.newFilledLike(ZTensor.newZeros(2, 1), 9))
      .isEqualTo(ZTensor.newFilled(new int[] { 2, 1 }, 9));

    // Fill on subview
    {
      var t = ZTensor.newZeros(2, 3);

      t.selectDim(0, 0).fill(2);
      assertThat(t).isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 2, 2 }, { 0, 0, 0 } }));
    }

    // CellGenerator
    {
      var t = ZTensor.newZeros(2, 3);
      CellGenerator cellGenerator = IndexingFns::intSum;
      t.fill(cellGenerator);
      assertThat(t)
        .isEqualTo(ZTensor.newFilled(new int[] { 2, 3 }, cellGenerator))
        .isEqualTo(ZTensor.newFilledLike(t, cellGenerator))
        .isEqualTo(ZTensor.newFromArray(new int[][] { { 0, 1, 2 }, { 1, 2, 3 } }));
    }

    // IntSupplier
    {
      var vals = List.of(1, 2, 3, 4, 5, 6);
      var t = ZTensor.newZeros(2, 3);
      t.fill(vals.iterator()::next);
      assertThat(t)
        .isEqualTo(ZTensor.newFilled(new int[] { 2, 3 }, vals.iterator()::next))
        .isEqualTo(ZTensor.newFilledLike(t, vals.iterator()::next))
        .isEqualTo(ZTensor.newMatrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }));
    }

    {
      assertThat(ZTensor.newFilled(new int[] { 2, 3 }, new AtomicInteger(0)::incrementAndGet))
        .isEqualTo(ZTensor.newFromArray(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }));
    }
  }

  @Test
  public void test_diagonal() {
    assertThat(ZTensor.newDiagonalMatrix()).isEqualTo(ZTensor.newZeros(0, 0));
    assertThat(ZTensor.newDiagonalMatrix(2, 3, 4))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 0, 0 }, { 0, 3, 0 }, { 0, 0, 4 } }));
  }

  @Test
  public void test_identity() {
    assertThat(ZTensor.newIdentityMatrix(0)).isEqualTo(ZTensor.newZeros(0, 0));
    assertThat(ZTensor.newIdentityMatrix(3))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }));
  }

  @Test
  public void test_selectDim() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });

    assertThat(t.selectDim(0, 0)).isEqualTo(ZTensor.newFromArray(new int[] { 2, 3 }));
    assertThat(t.selectDim(0, 1)).isEqualTo(ZTensor.newFromArray(new int[] { 4, 5 }));
    assertThat(t.selectDim(1, 0)).isEqualTo(ZTensor.newFromArray(new int[] { 2, 4 }));
    assertThat(t.selectDim(1, 1)).isEqualTo(ZTensor.newFromArray(new int[] { 3, 5 }));
  }

  @Test
  public void test_selectDims() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });

    assertThat(t.selectDims(new int[] { 0 }, new int[] { 0 }))
      .isEqualTo(ZTensor.newFromArray(new int[] { 2, 3 }));
    assertThat(t.selectDims(new int[] { -2 }, new int[] { 0 }))
      .isEqualTo(ZTensor.newFromArray(new int[] { 2, 3 }));
    assertThat(t.selectDims(new int[] { 0, 1 }, new int[] { 1, 0 }))
      .isEqualTo(ZTensor.newFromArray(4));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> t.selectDims(new int[] { 0, 1 }, new int[] { 1, 0, 1 }))
      .withMessageContaining("dims.length (2) != indexes.length (3)");
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
      .isThrownBy(() -> t.selectDims(new int[] { 3 }, new int[] { 0 }))
      .withMessageContaining("invalid dimension");
  }

  @Test
  public void test_permute() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3 }, { 4, 5 } }, { { 6, 7 }, { 8, 9 } } });

    assertThat(t.permute(0, 1, 2)).isEqualTo(t);

    assertThat(t.permute(0, 2, 1))
      .isEqualTo(
        ZTensor.newFromArray(new int[][][] { { { 2, 4 }, { 3, 5 } }, { { 6, 8 }, { 7, 9 } } })
      );
  }

  @Test
  public void test_reorderDim() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3 }, { 4, 5 } }, { { 6, 7 }, { 8, 9 } } });

    var r = t.reorderedDimCopy(new int[] { 1, 0 }, 1);
    assertThat(r)
      .isEqualTo(
        ZTensor.newFromArray(new int[][][] { { { 4, 5 }, { 2, 3 } }, { { 8, 9 }, { 6, 7 } } })
      );
  }

  @Test
  public void test_transpose() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3, 4 }, { 5, 6, 7 } } });
    assertThat(t.shapeAsArray()).isEqualTo(new int[] { 1, 2, 3 });

    {
      // no-op case.
      assertThat(t.transpose(1, 1)).isSameAs(t);
      assertThat(t.transpose(-2, 1)).isSameAs(t);
      assertThat(t.transpose(-2, -2)).isSameAs(t);
    }

    {
      // No arguments
      var trans = t.transpose();
      assertThat(trans.shapeAsArray()).isEqualTo(new int[] { 3, 2, 1 });

      assertThat(trans).isEqualTo(t.T());

      assertThat(trans)
        .isEqualTo(
          ZTensor.newFromArray(
            new int[][][] { { { 2 }, { 5 } }, { { 3 }, { 6 } }, { { 4 }, { 7 } } }
          )
        );
    }

    {
      // arguments
      var trans = t.transpose(1, 0);
      assertThat(trans.shapeAsArray()).isEqualTo(new int[] { 2, 1, 3 });

      assertThat(trans)
        .isEqualTo(ZTensor.newFromArray(new int[][][] { { { 2, 3, 4 } }, { { 5, 6, 7 } } }));
    }
  }

  @Test
  public void test_reverse() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3, 4 }, { 5, 6, 7 } } });

    assertThat(t.reverse(0))
      .isEqualTo(ZTensor.newFromArray(new int[][][] { { { 2, 3, 4 }, { 5, 6, 7 } } }));
    assertThat(t.reverse(1))
      .isEqualTo(ZTensor.newFromArray(new int[][][] { { { 5, 6, 7 }, { 2, 3, 4 } } }));
    assertThat(t.reverse(2))
      .isEqualTo(ZTensor.newFromArray(new int[][][] { { { 4, 3, 2 }, { 7, 6, 5 } } }));
  }

  @Test
  public void test_unsqueeze() {
    var t = ZTensor.newFromArray(new int[] { 2, 3, 4 });

    assertThat(t.unsqueeze(0)).isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 3, 4 } }));
    assertThat(t.unsqueeze(1)).isEqualTo(ZTensor.newFromArray(new int[][] { { 2 }, { 3 }, { 4 } }));
    assertThat(t.unsqueeze(-1))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2 }, { 3 }, { 4 } }));

    assertThat(t.unsqueeze(1).squeeze(1)).isEqualTo(t);

    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> t.unsqueeze(4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> t.unsqueeze(-3));
  }

  @Test
  public void test_squeeze() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3, 4 } });

    assertThat(t.squeeze(0)).isEqualTo(ZTensor.newFromArray(new int[] { 2, 3, 4 }));
    assertThat(t.squeeze(-2)).isEqualTo(ZTensor.newFromArray(new int[] { 2, 3, 4 }));

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> t.squeeze(1));
  }

  @Test
  public void test_broadcastDim() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3 } });

    assertThat(t.broadcastDim(0, 2))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 3 }, { 2, 3 } }));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> t.broadcastDim(1, 2))
      .withMessageContaining("Cannot broadcast dimension 1 with real-size 2");
  }

  @Test
  public void test_broadcastTo() {
    var t = ZTensor.newFromArray(new int[][] { { 2, 3 } });

    assertThat(t.isBroadcastDim(0)).isFalse();
    assertThat(t.isBroadcastDim(1)).isFalse();

    var bview = t.broadcastTo(2, 2);
    assertThat(bview).isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 3 }, { 2, 3 } }));

    assertThat(bview.isBroadcastDim(0)).isTrue();
    assertThat(bview.isBroadcastDim(1)).isFalse();

    bview.set(new int[] { 0, 0 }, 1);
    assertThat(bview).isEqualTo(ZTensor.newFromArray(new int[][] { { 1, 3 }, { 1, 3 } }));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> t.broadcastTo(2))
      .withMessageContaining("Cannot broadcast shape [1, 2] to [2]");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> t.broadcastTo(2, 3))
      .withMessageContaining("Cannot broadcast shape [1, 2] to [2, 3]");
  }

  @Test
  public void test_assign() {
    var t = ZTensor.newZeros(2, 3);

    t.selectDim(0, 0).assign_(ZTensor.newVector(1, 2, 3));
    assertThat(t).isEqualTo(ZTensor.newFromArray(new int[][] { { 1, 2, 3 }, { 0, 0, 0 } }));
  }

  @Test
  public void test_Ops_map() {
    assertThat(CellWiseOps.map(x -> x + 2, ZTensor.newScalar(4))).isEqualTo(ZTensor.newScalar(6));
    assertThat(CellWiseOps.map(x -> x + 2, ZTensor.newVector())).isEqualTo(ZTensor.newVector());
    assertThat(CellWiseOps.map(x -> x + 2, ZTensor.newVector(2, 3)))
      .isEqualTo(ZTensor.newVector(4, 5));
  }

  @Test
  public void test_neg() {
    assertThat(CellWiseOps.neg(ZTensor.newScalar(4))).isEqualTo(ZTensor.newScalar(-4));
    assertThat(CellWiseOps.neg(ZTensor.newVector())).isEqualTo(ZTensor.newVector());
    assertThat(CellWiseOps.neg(ZTensor.newVector(2, 3))).isEqualTo(ZTensor.newVector(-2, -3));

    assertThat(ZTensor.newScalar(4).neg()).isEqualTo(ZTensor.newScalar(-4));
    assertThat(ZTensor.newVector().neg()).isEqualTo(ZTensor.newVector());
    assertThat(ZTensor.newVector(2, 3).neg()).isEqualTo(ZTensor.newVector(-2, -3));
  }

  @Test
  public void test_abs() {
    assertThat(CellWiseOps.abs(ZTensor.newScalar(4))).isEqualTo(ZTensor.newScalar(4));
    assertThat(CellWiseOps.abs(ZTensor.newScalar(-4))).isEqualTo(ZTensor.newScalar(4));
    assertThat(CellWiseOps.abs(ZTensor.newVector())).isEqualTo(ZTensor.newVector());
    assertThat(CellWiseOps.abs(ZTensor.newVector(2, -3))).isEqualTo(ZTensor.newVector(2, 3));

    assertThat(ZTensor.newScalar(4).abs()).isEqualTo(ZTensor.newScalar(4));
    assertThat(ZTensor.newScalar(-4).abs()).isEqualTo(ZTensor.newScalar(4));
    assertThat(ZTensor.newVector().abs()).isEqualTo(ZTensor.newVector());
    assertThat(ZTensor.newVector(2, -3).abs()).isEqualTo(ZTensor.newVector(2, 3));
  }

  @Test
  public void test_reduceCells() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3 }, { 4, 5 } }, { { 6, 7 }, { 8, 9 } } });

    assertThat(t.reduceCellsAtomic(Integer::sum, 0)).isEqualTo(44);
    assertThat(t.reduceCells(Integer::sum, 0)).isEqualTo(ZTensor.newScalar(44));
    assertThat(t.reduceCells(Integer::sum, 0, 2))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 5, 9 }, { 13, 17 } }));
    assertThat(t.reduceCells(Integer::sum, 0, 0, 1, 2)).isEqualTo(ZTensor.newScalar(44));
  }

  @Test
  public void test_sum() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3 }, { 4, 5 } }, { { 6, 7 }, { 8, 9 } } });
    assertThat(t.sumAsInt()).isEqualTo(44);
    assertThat(t.sum()).isEqualTo(ZTensor.newScalar(44));
    assertThat(t.sum(0, 1, 2)).isEqualTo(ZTensor.newScalar(44));

    assertThat(t.sum(2)).isEqualTo(ZTensor.newFromArray(new int[][] { { 5, 9 }, { 13, 17 } }));
  }

  @Test
  public void test_prod() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3 }, { 4, 5 } }, { { 6, 7 }, { 8, 9 } } });
    assertThat(t.prodAsInt()).isEqualTo(362880);
    assertThat(t.prod()).isEqualTo(ZTensor.newScalar(362880));
    assertThat(t.prod(0, 1, 2)).isEqualTo(ZTensor.newScalar(362880));

    assertThat(t.prod(2)).isEqualTo(ZTensor.newFromArray(new int[][] { { 6, 20 }, { 42, 72 } }));
  }

  @Test
  public void test_min() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3 }, { 4, 5 } }, { { 6, 7 }, { 8, 9 } } });
    assertThat(t.minAsInt()).isEqualTo(2);
    assertThat(t.min()).isEqualTo(ZTensor.newScalar(2));
    assertThat(t.min(0, 1, 2)).isEqualTo(ZTensor.newScalar(2));

    assertThat(t.min(2)).isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 4 }, { 6, 8 } }));
  }

  @Test
  public void test_max() {
    var t = ZTensor.newFromArray(new int[][][] { { { 2, 3 }, { 4, 5 } }, { { 6, 7 }, { 8, 9 } } });
    assertThat(t.maxAsInt()).isEqualTo(9);
    assertThat(t.max()).isEqualTo(ZTensor.newScalar(9));
    assertThat(t.max(0, 1, 2)).isEqualTo(ZTensor.newScalar(9));

    assertThat(t.max(2)).isEqualTo(ZTensor.newFromArray(new int[][] { { 3, 5 }, { 7, 9 } }));
  }

  @Test
  public void test_ops_zipWith() {
    IntBinaryOperator fn = (x, y) -> x + 2 * y;

    {
      var empty = ZTensor.newVector();
      var lhs = ZTensor.newVector(3, 2);

      // [2], [2]
      var rhs = ZTensor.newVector(-1, 9);
      assertThat(CellWiseOps.zipWith(fn, empty, empty)).isEqualTo(empty);

      assertThat(CellWiseOps.zipWith(fn, lhs, rhs).toArray()).isEqualTo(new int[] { 1, 20 });
      assertThatThrownBy(() -> CellWiseOps.zipWith(fn, lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2], [0]");

      // Broadcast rules.
      // [2, 1], [2]
      assertThat(
        CellWiseOps.zipWith(
          Integer::sum,
          ZTensor.newFromArray(new int[][] { { 1 }, { 2 } }),
          ZTensor.newVector(3, 4)
        )
      )
        .isEqualTo(ZTensor.newFromArray(new int[][] { { 4, 5 }, { 5, 6 } }));
      assertThat(
        CellWiseOps.zipWith(
          Integer::sum,
          ZTensor.newFromArray(new int[][] { { 1 }, { 2 } }),
          ZTensor.newScalar(5)
        )
      )
        .isEqualTo(ZTensor.newFromArray(new int[][] { { 6 }, { 7 } }));

      // [2], <scalar>
      assertThat(CellWiseOps.zipWith(fn, empty, 12)).isEqualTo(empty);
      assertThat(CellWiseOps.zipWith(fn, lhs, 12).toArray()).isEqualTo(new int[] { 27, 26 });

      // <scalar>, [2]
      assertThat(CellWiseOps.zipWith(fn, 12, empty)).isEqualTo(empty);
      assertThat(CellWiseOps.zipWith(fn, 12, lhs).toArray()).isEqualTo(new int[] { 18, 16 });
    }

    {
      var empty = ZTensor.newMatrix();
      var lhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });

      // [2, 2], [2, 2]
      var rhs = ZTensor.newFromArray(new int[][] { { -1, 9 }, { 2, 0 } });
      assertThat(CellWiseOps.zipWith(fn, empty, empty)).isEqualTo(empty);
      assertThat(CellWiseOps.zipWith(fn, lhs, rhs).toArray())
        .isEqualTo(new int[][] { { 1, 20 }, { 5, 1 } });
      assertThatThrownBy(() -> CellWiseOps.zipWith(fn, lhs, empty))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

      // [2, 2], <scalar>
      assertThat(CellWiseOps.zipWith(fn, empty, 12)).isEqualTo(empty);
      assertThat(CellWiseOps.zipWith(fn, lhs, 12).toArray())
        .isEqualTo(new int[][] { { 27, 26 }, { 25, 25 } });

      // <scalar>, [2, 2]
      assertThat(CellWiseOps.zipWith(fn, 12, empty)).isEqualTo(empty);
      assertThat(CellWiseOps.zipWith(fn, 12, lhs).toArray())
        .isEqualTo(new int[][] { { 18, 16 }, { 14, 14 } });
    }
  }

  @Test
  public void test_map() {
    var t = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });
    assertThat(t.map(x -> x + 2))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 5, 4 }, { 3, 3 } }));
  }

  @Test
  public void test_map_() {
    var t = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });
    t.map_(x -> x + 2);
    assertThat(t).isEqualTo(ZTensor.newFromArray(new int[][] { { 5, 4 }, { 3, 3 } }));
  }

  @Test
  public void test_zipWith() {
    var t = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });
    assertThat(t.zipWith(Integer::sum, ZTensor.newFromArray(new int[][] { { 1, 2 }, { 3, 4 } })))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 4, 4 }, { 4, 5 } }));

    assertThat(t.zipWith(Integer::sum, ZTensor.newFromArray(new int[] { 1, 2 })))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 4, 4 }, { 2, 3 } }));
  }

  @Test
  public void test_minimum() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.minimum(empty, empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { -1, 2 }, { 1, 0 } });
    assertThat(CellWiseOps.minimum(lhs, rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { -1, 2 }, { 1, 0 } }));

    assertThatThrownBy(() -> CellWiseOps.minimum(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.minimum(empty, 2)).isEqualTo(empty);

    assertThat(CellWiseOps.minimum(lhs, 2))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 2 }, { 1, 1 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.minimum(2, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.minimum(2, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 2 }, { 1, 1 } }));
  }

  @Test
  public void test_maximum() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.maximum(empty, empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { -1, 2 }, { 1, 6 } });
    assertThat(CellWiseOps.maximum(lhs, rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 6 } }));

    assertThatThrownBy(() -> CellWiseOps.maximum(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.maximum(empty, 2)).isEqualTo(empty);

    assertThat(CellWiseOps.maximum(lhs, 2))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 3, 2 }, { 2, 2 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.maximum(2, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.maximum(2, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 3, 2 }, { 2, 2 } }));
  }

  @Test
  public void test_add() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.add(empty, empty)).isEqualTo(empty.add(empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { -1, 9 }, { 2, 0 } });
    assertThat(CellWiseOps.add(lhs, rhs))
      .isEqualTo(lhs.add(rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 11 }, { 3, 1 } }));

    assertThatThrownBy(() -> CellWiseOps.add(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.add(empty, 12)).isEqualTo(empty.add(12)).isEqualTo(empty);

    assertThat(CellWiseOps.add(lhs, 12))
      .isEqualTo(lhs.add(12))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 15, 14 }, { 13, 13 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.add(12, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.add(12, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 15, 14 }, { 13, 13 } }));

    var inplace = lhs.clone();
    CellWiseOps.add_(inplace, rhs);
    CellWiseOps.add_(inplace, 12);
    inplace.add_(rhs);
    inplace.add_(13);
    assertThat(inplace).isEqualTo(lhs.add(rhs).add(12).add(rhs).add(13));
  }

  @Test
  public void test_sub() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.sub(empty, empty)).isEqualTo(empty.sub(empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { -1, 9 }, { 2, 0 } });
    assertThat(CellWiseOps.sub(lhs, rhs))
      .isEqualTo(lhs.sub(rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 4, -7 }, { -1, 1 } }));

    assertThatThrownBy(() -> CellWiseOps.sub(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.sub(empty, 12)).isEqualTo(empty.sub(12)).isEqualTo(empty);

    assertThat(CellWiseOps.sub(lhs, 12))
      .isEqualTo(lhs.sub(12))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { -9, -10 }, { -11, -11 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.sub(12, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.sub(12, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 9, 10 }, { 11, 11 } }));

    var inplace = lhs.clone();
    CellWiseOps.sub_(inplace, rhs);
    CellWiseOps.sub_(inplace, 12);
    inplace.sub_(rhs);
    inplace.sub_(13);
    assertThat(inplace).isEqualTo(lhs.sub(rhs).sub(12).sub(rhs).sub(13));
  }

  @Test
  public void test_mul() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 1 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.mul(empty, empty)).isEqualTo(empty.mul(empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { -1, 9 }, { 2, 0 } });
    assertThat(CellWiseOps.mul(lhs, rhs))
      .isEqualTo(lhs.mul(rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { -3, 18 }, { 2, 0 } }));

    assertThatThrownBy(() -> CellWiseOps.mul(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.mul(empty, 12)).isEqualTo(empty.mul(12)).isEqualTo(empty);

    assertThat(CellWiseOps.mul(lhs, 12))
      .isEqualTo(lhs.mul(12))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 36, 24 }, { 12, 12 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.mul(12, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.mul(12, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 36, 24 }, { 12, 12 } }));

    var inplace = lhs.clone();
    CellWiseOps.mul_(inplace, rhs);
    CellWiseOps.mul_(inplace, 12);
    inplace.mul_(rhs);
    inplace.mul_(13);
    assertThat(inplace).isEqualTo(lhs.mul(rhs).mul(12).mul(rhs).mul(13));
  }

  @Test
  public void test_div() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 24, 12 }, { 9, 1 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.div(empty, empty)).isEqualTo(empty.div(empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { -1, 9 }, { 2, 1 } });
    assertThat(CellWiseOps.div(lhs, rhs))
      .isEqualTo(lhs.div(rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { -24, 1 }, { 4, 1 } }));

    assertThatThrownBy(() -> CellWiseOps.div(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.div(empty, 12)).isEqualTo(empty.div(12)).isEqualTo(empty);

    assertThat(CellWiseOps.div(lhs, 12))
      .isEqualTo(lhs.div(12))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 1 }, { 0, 0 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.div(12, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.div(12, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 0, 1 }, { 1, 12 } }));

    // Div by 0
    assertThatThrownBy(() -> CellWiseOps.div(lhs, ZTensor.newZerosLike(lhs)))
      .isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> CellWiseOps.div(lhs, 0)).isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> CellWiseOps.div(12, ZTensor.newZerosLike(lhs)))
      .isInstanceOf(ArithmeticException.class);

    var inplace = lhs.mul(12345);
    CellWiseOps.div_(inplace, rhs);
    CellWiseOps.div_(inplace, 12);
    inplace.div_(rhs);
    inplace.div_(13);
    assertThat(inplace).isEqualTo(lhs.mul(12345).div(rhs).div(12).div(rhs).div(13));
  }

  @Test
  public void test_mod() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 24, 12 }, { 9, 1 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.mod(empty, empty)).isEqualTo(empty.mod(empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { -1, 9 }, { 2, 1 } });
    assertThat(CellWiseOps.mod(lhs, rhs))
      .isEqualTo(lhs.mod(rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 0, 3 }, { 1, 0 } }));

    assertThatThrownBy(() -> CellWiseOps.mod(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.mod(empty, 12)).isEqualTo(empty.mod(12)).isEqualTo(empty);

    assertThat(CellWiseOps.mod(lhs, 12))
      .isEqualTo(lhs.mod(12))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 0, 0 }, { 9, 1 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.mod(12, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.mod(12, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 12, 0 }, { 3, 0 } }));

    // mod by 0
    assertThatThrownBy(() -> CellWiseOps.mod(lhs, ZTensor.newZerosLike(lhs)))
      .isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> CellWiseOps.mod(lhs, 0)).isInstanceOf(ArithmeticException.class);

    assertThatThrownBy(() -> CellWiseOps.mod(12, ZTensor.newZerosLike(lhs)))
      .isInstanceOf(ArithmeticException.class);

    var inplace = lhs.mul(12345);
    CellWiseOps.mod_(inplace, rhs);
    CellWiseOps.mod_(inplace, 12);
    inplace.mod_(rhs);
    inplace.mod_(13);
    assertThat(inplace).isEqualTo(lhs.mul(12345).mod(rhs).mod(12).mod(rhs).mod(13));
  }

  @Test
  public void test_pow() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 5 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.pow(empty, empty)).isEqualTo(empty.pow(empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 0 } });
    assertThat(CellWiseOps.pow(lhs, rhs))
      .isEqualTo(lhs.pow(rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 8, 9 }, { 4, 1 } }));

    assertThatThrownBy(() -> CellWiseOps.pow(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.pow(empty, 12)).isEqualTo(empty.pow(12)).isEqualTo(empty);

    assertThat(CellWiseOps.pow(lhs, 12))
      .isEqualTo(lhs.pow(12))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 4096, 531441 }, { 16777216, 244140625 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.pow(12, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.pow(12, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 144, 1728 }, { 20736, 248832 } }));

    var inplace = lhs.mul(12345);
    CellWiseOps.pow_(inplace, rhs);
    CellWiseOps.pow_(inplace, 12);
    inplace.pow_(rhs);
    inplace.pow_(13);
    assertThat(inplace).isEqualTo(lhs.mul(12345).pow(rhs).pow(12).pow(rhs).pow(13));
  }

  @Test
  public void test_log() {
    var empty = ZTensor.newZeros(0, 0);
    var lhs = ZTensor.newFromArray(new int[][] { { 2, 3 }, { 4, 20 } });

    // [2, 2], [2, 2]
    assertThat(CellWiseOps.log(empty, empty)).isEqualTo(empty.log(empty)).isEqualTo(empty);

    var rhs = ZTensor.newFromArray(new int[][] { { 3, 2 }, { 2, 2 } });
    assertThat(CellWiseOps.log(lhs, rhs))
      .isEqualTo(lhs.log(rhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 0, 1 }, { 2, 4 } }));

    assertThatThrownBy(() -> CellWiseOps.log(lhs, empty))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cannot broadcast shapes: [2, 2], [0, 0]");

    // [2, 2], <scalar>
    assertThat(CellWiseOps.log(empty, 12)).isEqualTo(empty.log(12)).isEqualTo(empty);

    assertThat(CellWiseOps.log(lhs, 12))
      .isEqualTo(lhs.log(12))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 0, 0 }, { 0, 1 } }));

    // <scalar>, [2, 2]
    assertThat(CellWiseOps.log(12, empty)).isEqualTo(empty);

    assertThat(CellWiseOps.log(12, lhs))
      .isEqualTo(ZTensor.newFromArray(new int[][] { { 3, 2 }, { 1, 0 } }));

    var inplace = lhs.mul(12345);
    CellWiseOps.log_(inplace, rhs);
    CellWiseOps.log_(inplace, 12);
    inplace.log_(rhs);
    inplace.log_(13);
    assertThat(inplace).isEqualTo(lhs.mul(12345).log(rhs).log(12).log(rhs).log(13));
  }

  @Test
  public void test_mutable() {
    var tensor = ZTensor.newFromArray(new int[][] { { 1, 2 }, { 3, 4 } });
    assertThat(tensor.isMutable()).isTrue();
    assertThat(tensor.isReadOnly()).isFalse();

    //noinspection ResultOfMethodCallIgnored
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(tensor::hashCode)
      .withMessageContaining("mutable");

    assertThat(tensor.isMutable()).isTrue();
    tensor.assertMutable();
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(tensor::assertReadOnly)
      .withMessageContaining("mutable");

    tensor.set(new int[] { 0, 0 }, 5);
    assertThat(tensor).isEqualTo(ZTensor.newFromArray(new int[][] { { 5, 2 }, { 3, 4 } }));

    var fixed = tensor.asImmutable();
    assertThat(fixed.isMutable()).isFalse();
    assertThat(fixed.isReadOnly()).isTrue();

    fixed.assertReadOnly();
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(fixed::assertMutable)
      .withMessageContaining("immutable");

    assertThat(fixed).isNotSameAs(tensor).extracting(ZTensor::isMutable).isEqualTo(false);
    assertThat(fixed.asImmutable()).isSameAs(fixed);
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> fixed.set(new int[] { 0, 0 }, 5))
      .withMessageContaining("immutable");

    assertThat(fixed.hashCode()).isEqualTo(tensor.clone(false).hashCode());
  }

  @Test
  public void test_mutable_view() {
    var tensor = ZTensor.newZeros(2, 3);
    var view = tensor.T();
    tensor.set(new int[] { 1, 0 }, 3);

    assertThat(view).isEqualTo(ZTensor.newFromArray(new int[][] { { 0, 3 }, { 0, 0 }, { 0, 0 } }));

    view.add_(2);

    assertThat(tensor).isEqualTo(ZTensor.newFromArray(new int[][] { { 2, 2, 2 }, { 5, 2, 2 } }));
  }

  public static class JsonExampleContainer {

    public ZTensor tensor;

    //noinspection unused
    @JsonCreator
    public JsonExampleContainer() {}

    public JsonExampleContainer(ZTensor tensor) {
      this.tensor = tensor;
    }

    @Override
    public String toString() {
      return "ExampleContainer{" + "tensor=" + tensor + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonExampleContainer that)) return false;
      return Objects.equals(tensor, that.tensor);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tensor);
    }
  }

  @Test
  public void test_toFlatArray() {
    var tensor = ZTensor
      .newFromArray(new int[][] { { 1, 2 }, { 3, 4 } })
      .reverse(1)
      .selectDim(0, 0);

    assertThat(tensor).isEqualTo(ZTensor.newFromArray(new int[] { 2, 1 }));

    assertThat(tensor).isEqualTo(ZTensor.newFromFlatArray_(tensor.toFlatArray()));

    assertThat(tensor.toFlatArray()).isEqualTo(new FlatArray(new int[] { 2 }, new int[] { 2, 1 }));
  }
}
