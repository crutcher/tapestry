# Tapestry Project Overview

- [Introduction](#introduction)
- [Motivation](#motivation)
- [Modular Graph Representation](#modular-graph-representation)

## Introduction

> The goal of the **Tapestry Project** is to develop a complete and developer-friendly toolchain for
> generating, visualizing, transforming, compiling, and optimizing polyhedral type tensor block
> algebra expressions into optimized code for a variety of target architectures.

That's a mouthful, so let's break it down.

A _tensor algebra expression_ is a mathematical expression that involves tensors, which are
multidimensional arrays of numbers. For example, a matrix is a 2-dimensional tensor, and a vector is
a 1-dimensional tensor. Tensor algebra is a generalization of matrix algebra, and is used in many
scientific and engineering applications, such as artificial intelligence, quantum mechanics, fluid
dynamics, and computer graphics.

An expression in a tensor algebra derives its value from a series of functional operations performed
on one or more tensors; and producing one or more tensors, for example, consider a basic matrix
multiplication:

```
A := <Tensor of size (m, n)>
B := <Tensor of size (n, p)>

C = MatMul(A, B)
# C := <Tensor of size (m, p)>
```

In compiler optimization, it is often useful to produce re-write rules, which state that one general
form of an expression can be transformed into another form that is at least equivalent, and
preferably more efficient to calculate. For example, the above expression can be re-written as a
composition of two operations (which in this case will probably not produce any benefit):

```
A := <Tensor of size (m, n)>
B := <Tensor of size (n, p)>

Z = Prod(A, B)
# Z := <Tensor of size (m, n, p)>

C = RowSum(Z, axis=1)
# C := <Tensor of size (m, p)>
```

Lacking further visibility into the internals of the operations, optimizers are limited to
re-writing expressions based on these re-write rules; and on altering where and when operations are
scheduled to run.

If we observe that many tensor operations are _block_ operations, in that they operate independently
on subsets of their inputs in such a way that it is possible to split them into smaller operations
and re-combine the results, we begin to see that there is a potential for optimization which looks
inside the operations in its restructuring.

The _polyhedral model_ or _[polytope model](https://en.wikipedia.org/wiki/Polytope_model)_ provides
a framework for describing _some_ block operations in a way which permits direct reasoning about
sub-sharding and recombination of the operations; without knowledge of the internals of the
operation itself.

The term _polyhedral type signature_ has come to be used to describe the spatial type of an
operation as it is described in the polyhedral model. This is a generalization of the term _block
operation_ to include the spatial type of the operation.

By extending a tensor block algebra with polyhedral type signatures, we can describe expressions of
block operations in a way that permits direct reasoning about sub-sharding and recombination of the
component operations, in addition to the above graph re-writing and scheduling.

```
A := <Tensor of size (m, n)>
B := <Tensor of size (n, p)>

C0 = MatMul(A, B[1:k])
# C0 := <Tensor of size (m, k)>

C1 = MatMul(A, B[k:])
# C1 := <Tensor of size (m, p - k)>

C = Concatenate([C0, C1], axis=1)
# C := <Tensor of size (m, p)>
```

This is discussed in much greater detail in the
[Polyhedral Types and Index Projection](PolyhedralTypesAndIndexProjection.md) document.

A _polyhedral type tensor block algebra_ optimizer toolchain is directly analogous to the SQL model;
where the SQL language permits the user to describe the _what_ of a query in terms of _relational
algebra_, and the SQL engine, by applying aggressive
[query optimization](https://en.wikipedia.org/wiki/Query_optimization) arrives at a
[query plan](https://en.wikipedia.org/wiki/Query_plan) which is equivalent to the original query,
but is more efficient to execute.

## Motivation

The space of GPU-accelerated tensor environments is already dominated by a few well-known
ecosystems; most notably [PyTorch](https://pytorch.org/), [TensorFlow](https://www.tensorflow.org/),
and [Jax](https://jax.readthedocs.io/).

These projects all started with an API surface they wished to replicate, namely the
[NumPy API](https://numpy.org/), which is a popular library for numerical computing in Python; and
is itself a partial clone of [R](https://www.r-project.org/), a popular language for statistical
computing. These statistical libraries have deep roots in the statistical and artificial
intelligence communities, and have been optimized for the ease of use of researchers, analysts, and
developers working in statistics, signal processing, and artificial intelligence.

As the primary goal of the extant GPU-accelerated tensor environments was to support acceleration of
research communities which were already working in numpy, R, and other similar languages, the
initial versions of these libraries focused heavily on providing a drop-in replacement for numpy,
and on providing a similar high-level API for the manipulation of tensors.

Optimization-friendly expression algebras (which support aggressive re-write operations) require a
significant amount of additional information about the operations being performed, and place strong
restrictions on the families of operations which are even permitted.

Worse, due to the restrictions, it is quite difficult to retrofit an existing tensor algebra with
the necessary information to support aggressive re-write operations; as it is quite likely than many
existing operations in the algebra to be retrofitted will not be compatible with the necessary
restrictions.

**NumPy** wasn't written to be optimized in this way. A large portion of the api surface is
compatible with the necessary restrictions, but a significant portion is not. The same is true of
the libraries which were written as developer api replacements for numpy.

At the same time, the total investment being spent on power and compute resources for large tensor
operations is growing rapidly; even small improvements in the efficiency of the above libraries
translates into millions of dollars of savings in power and compute resources.

Their development budgets are driven by small scale gains with massive compounding effects; and as a
result the teams are largely precluded from exploring ground-up re-writes of their tensor algebras
to support aggressive re-write operations.

## Modular Graph Representation

> **IR is Destiny.**

A compiler's internal representation (commonly called the
[intermediate representation](https://en.wikipedia.org/wiki/Intermediate_representation), as it
exists between the source code which was parsed and the target code to be generated) determines most
things about the complexity and capabilities of the compiler.

Information which can be retrieved or verified easily in an **IR** can be used for analysis and
manipulation with a little code; information which is fragile or difficult to retrieve or verify
requires a lot of code to work with. And code which is difficult to work with is code which is
difficult to maintain, and difficult to extend.

In targeting a toolchain which spans from abstract tensor algebra expressions to optimized code for
a variety of target architectures, the **IR** is the most important part of the toolchain; and the
ability to extend and constrain the **IR** for different layers of that toolchain, and for different
primitives appropriate to different target architectures, is the most important part of the **IR**.

Tapestry is designed with a modular **IR** which permits the easy addition of new node types, node
tags, and graph constraints. By selectively including a set of types and constraints, strictly
defined sub-dialects can be created which are appropriate for different layers of the toolchain, and
for different primitives appropriate to different target architectures.

In this way, toolchain operations which transform from one layer to another can be written in a
type-safe way which transform from one dialect to another; and targeted query, debugging, and
visualization tools can be written which are appropriate for the layer of the toolchain being
targeted.

As the core representation, serialization, and scanning code are shared by all dialects, much of the
verification and manipulation code can be shared as well; and the code which is not shared is
written in a type-safe way which is appropriate for the layer of the toolchain being targeted.

TBD

### Alternative IR

Many, many alternative representations were considered and experimented with.

TBD
