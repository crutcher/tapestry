# Tapestry Weft

**Weft** is a **Tapestry** module which provides a symbolic metakernel execution API for describing
computations.

A **metakernel** is the symbolic execution pair of a kernel. Given inputs and parameters, it
produces a graph description of the operation which is to be performed; the shape and types of the
inputs and outputs, the sharding index spaces and index projections appropriate for the operation,
and the constraints and requirements of the operation.

```java
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.weft.dialects.tensorops.AddKernel;

static class Example {

  public static void main(String[] args) {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var tensorA = TensorNode
      .builder(graph)
      .label("A")
      .body(c -> c.dtype("int32").shape(10, 10))
      .build();

    var tensorB = TensorNode
      .builder(graph)
      .label("B")
      .body(c -> c.dtype("int32").shape(10, 10))
      .build();

    var add = new AddKernel();

    var op = add
      .on(graph)
      .input("tensors", List.of(TensorSelection.from(tensorA), TensorSelection.from(tensorB)))
      .apply();

    graph.validate();
  }
}

```

![add.example](docs/media/add.example.jpg)
