# Tapestry ZSpace

**ZSpace** is a **Tapestry** module which provides integer space (Z-space) tensors.

It is useful when an application has to reason about z-space shapes and locations, ranges of
coordinates, slices of coordinate ranges, projections of coordinates from one index space to
another, and so on.

The ZSpace tensors are implemented entirely in java, with no JNI or GPU hardware dependencies or
acceleration. As the ZSpace tensors are implemented in Java, they are not as fast as native code or
GPU code, but they are portable and can be used in any Java environment.

Example usage:

```java
import java.util.concurrent.atomic.AtomicInteger;
import org.tensortapestry.zspace.ZTensor;

static class Example {

  public static void main(String[] args) {
    var t = ZTensor.newFilled(new int[] { 2, 4 }, new AtomicInteger(0)::incrementAndGet);
    System.out.println(t);
    // [[1, 2, 3, 4],
    //  [5, 6, 7, 8]]

    t.select(":, :2:2").add_(2);
    System.out.println(t);
    // [[3, 2, 5, 4],
    //  [7, 6, 9, 8]]
  }
}

```

Core Features:

- [ZTensor](../tensortapestry-zspace/src/main/java/org/tensortapestry/zspace/ZTensor.java):
  n-dimensional integer tensor.

  - Supports mutable and immutable tensors.
  - Supports broadcasting and element wise operations.
  - Supports slicing and selecting sub-tensor views.
  - Supports view manipulation and reshaping.
  - Supports serialization and deserialization to and from JSON, Java Arrays, and MsgPack.

- [ZPoint](../tensortapestry-zspace/src/main/java/org/tensortapestry/zspace/ZPoint.java): immutable
  coordinate

  - Supports point addition, subtraction, and scalar multiplication.
  - Supports dominance ordering point comparison and equality.

- [ZRange](../tensortapestry-zspace/src/main/java/org/tensortapestry/zspace/ZRange.java): immutable
  n-dimensional coordinate range

  - Supports range addition, subtraction, and scalar multiplication.
  - Supports range intersection, union, and containment.
  - Supports range iteration and enumeration.

- [ZMatrix](../tensortapestry-zspace/src/main/java/org/tensortapestry/zspace/ZMatrix.java):
  immutable 2-dimensional
- [ZAffineMap](../tensortapestry-zspace/src/main/java/org/tensortapestry/zspace/ZAffineMap.java):
  immutable affine coordinate projection
- [ZRangeProjectionMap](../tensortapestry-zspace/src/main/java/org/tensortapestry/zspace/ZRangeProjectionMap.java):
  ZAffineMap decorated with a shape.

## Json Serialization

Most ZSpace objects are tightly coupled with the Jackson JSON serialization library.

The following example demonstrates how to serialize and deserialize a ZTensor to and from JSON:

```java
import java.util.concurrent.atomic.AtomicInteger;
import org.tensortapestry.zspace.ZTensor;

static class Example {

  public static void main(String[] args) {
    var t = ZTensor.newFilled(new int[] { 2, 4 }, new AtomicInteger(0)::incrementAndGet);

    var json = t.toJsonString(); // [[1,2,3,4],[5,6,7,8]]

    var v = ZTensor.parse(json);

    assert t.equals(v);
  }
}

```
