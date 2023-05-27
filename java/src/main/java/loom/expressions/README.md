# Notes towards an Expression Language

This is a demo implementation of the ideas in [Tapestry](htttps://github.com/loom-ai/tapestry),
which is much more verbose than this stack.

These notes exist to help me argue through the object hierarchy; not to document correct usage.

Expression languages differ from process languages in that define values in terms of
transformations on previous values. The simplest outcome of this is that it's quite
easy to use a given value more than once; but by adding an observer, we can define
directly which values are ever observed by the outside world.

Values which are never observed are free to be inlined (when they contribute to other
values which transitively are observed), or even eliminated entirely (when they don't
contribute to any observed values).

So the simplest non-trivial tensor expression is a tensor which is observed directly:

```mermaid
flowchart LR
  classDef tensorValue fill:#99f,stroke:#333,stroke-width:4px,color:#000;
  
  A[["Tensor: A"]];
  class A tensorValue;
  
  obs(((observer)));
  A --o obs;
  
```

What does it mean for us to be able to observe a tensor value?

* After the expression is evaluated, we can read the value of the tensor.

We're generaly interested in more complex expressions, where transformations are applied to
tensor values, and then to the results of those transformations, and so on.

```mermaid
flowchart LR
  classDef tensorValue fill:#99f,stroke:#333,stroke-width:4px,color:#000;
  classDef exprValue fill:#9f9,stroke:#333,stroke-width:4px,color:#000;
  
  A[["Tensor: A"]]
  B[["Tensor: B"]]
  class A,B tensorValue;
  
  A --> X;
  B --> X;
  X[/"BlockExpr: X"/]
  class X exprValue;
  C[["Tensor: C"]]
  D[["Tensor: D"]]
  class C,D tensorValue;
  X --> C;
  X --> D;
  
  D --> Y;
  Y[/"Tensor: Y"/]
  class Y exprValue;
  E[["Tensor: E"]]
  class E tensorValue;
  Y --> E;
  
  obs(((observer)));
  E --o obs;
```

In this example, the *Tensor: C* value is never observed, and so it can dropped entirely
from our schedule, or generated and *written* to a null-store by the block expr.

Additionally, at this level it's quite possible that the tensors, and the inputs the *BlockExpr*
abstractions which could not fit on a single machine.

We are interested in the ability to:

* shard these operations and values;
* execute a given sharded schedule;
* and to compare the costs (in time and space) of different sharding choices.

```mermaid
flowchart LR
  classDef tensorValue fill:#99f,stroke:#333,stroke-width:4px,color:#000;
  classDef exprValue fill:#9f9,stroke:#333,stroke-width:4px,color:#000;
  
  subgraph A
  A.1[["Tensor: A.1"]]
  A.2[["Tensor: A.2"]]
  class A.1,A.2 tensorValue;
  end
  
  B[["Tensor: B.1"]]
  class B tensorValue;
  
  
  A.1 --> X.1;
  A.2 --> X.2;
  B --> X.1;
  B --> X.2;
  
  subgraph X
  X.1[/"BlockExpr: X.1"/]
  X.2[/"BlockExpr: X.2"/]
  class X.1,X.2 exprValue;
  end
  
  subgraph C
  C.1[["Tensor: C.1"]]
  C.2[["Tensor: C.2"]]
  class C.1,C.2 tensorValue;
  end
  
  subgraph D
  D.1[["Tensor: D.1"]]
  D.2[["Tensor: D.2"]]
  class D.1,D.2 tensorValue;
  end
  
  X.1 --> C.1
  X.2 --> C.2
  
  X.1 --> D.1
  X.2 --> D.2
  
  C.1 --> Y.1
  C.2 --> Y.2
  
  subgraph Y
  Y.1[/"BlockExpr: X.1"/]
  Y.2[/"BlockExpr: X.2"/]
  class Y.1,Y.2 exprValue;
  end
  
  subgraph E
  E.1[["Tensor: E.1"]]
  E.2[["Tensor: E.2"]]
  class E.1,E.2 tensorValue;
  end
  Y.1 --> E.1;
  Y.2 --> E.2;
  
  obs(((observer)));
  E --o obs;
```
