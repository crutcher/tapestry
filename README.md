# Tapestry Compiler Suite

**Tapestry** is an experimental optimizing tensor expression compiler suite.

![example](tensortapestry-loom/docs/media/example.svg)

The goal of **Tapestry** is to provide an ecosystem for a high-performance stochastic pareto-front
optimizer for distributed tensor expressions, targeting optimizations which are permitted to search
for extended time on a large number of machines.

Many modern tensor expressions may see 10k GPU-**years** of computation time over their lifetime,
such as a trained inference model hosted in production; and seeking optimizations which can reduce
this computation time by any extent is worth extensive optimization search.

It is built in several layers:

- an in-memory, GPU-free **Z**-space tensor math library. This provides a framework for reasoning
  about polyhedral types, coordinate-space ranges, and index projection functions.
- an extensible, JSON serializable **IR** (intermediate representation) for tensor expressions. This
  provides a framework for reasoning about tensor expressions, and for applying transformations to
  them.
- an extensible validation stack for **IR** expressions. This provides a framework for reasoning
  about the correctness of tensor expressions.

In the current stage of development, **Tapestry** is a research prototype. It lacks many features,
including code generation and optimization machinery.

See the [Tapestry Project Writeup](https://crutcher.github.io/Tapestry/) for a more detailed
overview of the theory and goals of **loom**. Note, this document predates much of the current
development, and is more of a research direction plan and overview of polyhedral optimization
theory.

## Sub-Projects

- **[ZSpace](tensortapestry-zspace/README.md)** - integer space (Z-space) tensors.
- **[loom](tensortapestry-loom/README.md)** - tensor expression graph representation.
- **[weft](tensortapestry-weft/README.md)** - metakernel symbolic execution api.

## Development Philosophy

The **Tapestry** values are, in order of precedence:

1. **readability** - it should be easy to understand what the code is doing.
2. **verifiability** - as much behavior as possible should be mechanically verifiable.
3. **visibility** - it should be easy to visualize what the code is doing.
4. **velocity** - it should be easy to add new features.
5. **extensibility** - it should be easy to add new features as plugins.
6. **correctness** - the algorithms should be correct.
7. **speed** - the algorithms should be fast.
8. **completeness** - the algorithms should be complete.

As the target for **Tapestry** is a high-throughput stochastic pareto-front optimizer; speed and
correctness are critically important; and that is why **Tapestry** is built in a compiled language.

However, **speed** and **correctness** are **products** of the ability of researchers and developers
to understand, verify, visualize, and extend the code.

## Getting Started

In the current stage of development, **loom** produces no tool targets; and exists solely as a
collection of libraries and tests.

## Active Work Surfaces

### Docs

The existing **Tapestry** writeup is a project plan and overview of the background an polyhedral
type theory. It predates much of the current development, and is not a good guide to the current
state of the project.

A document written from the perspective of existing libraries only, walking through the code and
usage of the libraries, would be a better guide to the current state of the project.

### Stable Domain Name

I've registered the domain name `tensortapestry.org`; but but it is not yet bound to any content or
github site.

### Releasing Maven Packages

I've been working on code-quality/maven modularization, towards the goal of releasing maven
packages.

Once the domain name is bound, some of the more complete packages (such as
[ZSpace](tensortapestry-zspace/README.md)) can be published to maven.

### Loom URI Namespaces

[loom](tensortapestry-loom/README.md) currently uses JSD (Json Schema Document) URLs to define the
types of datastructures in the graph. Switching to a URI namespace would be more appropriate, and
permit multiple resources to be bound and resolved for a given environment from the same URI.

Switching to URIs would require locking in a resource namespace scheme; one which permitted resource
versioning, and domain name scoping, so that extensions could be added to the namespace cleanly.

It would also require updating the resource resolution machinery in loom to support the new
namespace scheme.

### Loom Graph Visualization Plugin-Based Refactor

The current graph visualization is a fast hack for demonstration; and a better thought out plugin
architecture for visualizers would improve the ability to add to and extend the graph.

### Weft Metakernel Library

A **metakernel** is the symbolic execution pair of a kernel. Given inputs and parameters, it
produces a graph description of the operation which is to be performed; the shape and types of the
inputs and outputs, the sharding index spaces and index projections appropriate for the operation,
and the constraints and requirements of the operation.

The **weft metakernel library** is a draft of a symbolic execution api for describing computations.

It needs more complete interfaces, and it needs a larger vocabulary of operations.

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
      .configure(c -> c.dtype("int32").shape(10, 10))
      .build();

    var tensorB = TensorNode
      .builder(graph)
      .label("B")
      .configure(c -> c.dtype("int32").shape(10, 10))
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

![add.example](tensortapestry-weft/docs/media/add.example.jpg)

### Weft Metakernel Template Language

The current [weft](tensortapestry-weft/README.md) metakernel language is an incomplete draft; and
needs to be flushed out for how operations such as `concat(ts, axis)` and
`split(t, chunk_size, axis)` are to be represented.

A **metakernel** is the symbolic execution pair of a kernel. Given inputs and parameters, it
produces a graph description of the operation which is to be performed; the shape and types of the
inputs and outputs, the sharding index spaces and index projections appropriate for the operation,
and the constraints and requirements of the operation.

**metakernels** can be implemented in code; but a more portable and extensible way to define them
would be to define a template language for them, and to implement an interpreter for the template
language.

Consider the following example of a draft metakernel for **matmul**, with batch broadcasting:

```yaml
matmul:
  index: "[$shape..., $a, $c]"

  constraints:
    dtype:
      enum:
        - int32
        - int64
        - float32
        - float64
        - complex64
        - complex128

  inputs:
    X:
      shape: "[$shape..., $a, $b]"
      dtype: "$dtype"

      # this is shorthand for:
      # ipf:
      #   map: "[..., 1, 0]"
      #   shape: "[..., 1, $b]"
      #
      # which is shorthand for:
      # ipf:
      #   map: "[ones($index.size - 2)..., 1, 0]"
      #   shape: "[ones($index.size - 2)..., 1, $b]"
      #
      # Where the map unpacks to a diagonal matrix
      # with 1s on the prefix dimensions.
      ipf: "[..., 1, 0] :> [..., 1, $b]"

    W:
      shape: "[$shape..., $b, $c]"
      dtype: "$dtype"
      ipf: "[..., 0, 1] :> [..., $b, 1]"

  outputs:
    result:
      # shape defaults to: "[$.index...]"
      # ipf defaults to: "[...] >: [...]"
      dtype: "$dtype"
```

### Graph Rewrite Template Language

I _suspect_ that a good graph rewrite template language can be built on top of a good metakernel
template language.

This work is defered until the metakernel template language is more complete.
