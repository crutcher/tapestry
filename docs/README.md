# Tapestry Project

The **Tapestry Project** is a research project to develop a complete and developer-friendly
toolchain for generating, visualizing, transforming, compiling, and optimizing polyhedral type
tensor block algebra expressions into optimized code for a variety of target architectures.

**Tapestry** heavily leverages
[Polyhedral Type Signatures](PolyhedralTypes/PolyhedralTypeSignatures.md) to model shardable tensor
block algebra expressions.

Read the [Tapestry Project Overview](TapestryProjectOverview.md) for an overview of the background
and goals of the project.

## Table of Contents

- [Overview](#overview)
- [Development Philosophy](#development-philosophy)
- [Sub-Projects](#sub-projects)

## Overview

Read the [Tapestry Project Overview](TapestryProjectOverview.md) for an overview of the background
and goals of the project.

> **Note**: Additionally, there is a historical pre-development writeup available at
> [Tapestry Project Writeup](https://crutcher.github.io/Tapestry/) for a more detailed overview
> (~100 pages) of the theory and goals of the project. Note that this document was written before
> the current development project, and is more of a research direction plan and overview of
> polyhedral optimization theory than a description of the current state of the project's
> architecture.

**Tapestry** is an experimental tensor expression optimizing compiler suite.

The goal of **Tapestry** is to provide an ecosystem for a high-performance stochastic pareto-front
optimizer for distributed tensor expressions, targeting optimizations which are permitted to search
for extended time on a large number of machines.

Many modern tensor expressions may see 10k GPU-**years** of computation time over their lifetime,
such as a trained inference model hosted in production; and seeking optimizations which can reduce
this computation time by any extent is worth extensive optimization search.

Roughly, the target layers of **Tapestry** are intended to be:

- coordinate space range and projection api for describing kernel index projections
- metakernel descriptions of tensor kernel operations
- symbolic execution api for metakernel expressions to static block operation graphs
- representation and manipulation api for static block operation graphs
- graph rewrite expressions over static block operation graphs
- execution cost models for target environments
- pareto optimization of execution graphs with respect to cost models
- code generation for optimized execution graphs in target environments

In the current stage of development, **Tapestry** is a research prototype. It lacks many features,
including code generation and optimization machinery.

**Tapestry** heavily leverages
[Polyhedral Type Signatures](PolyhedralTypes/PolyhedralTypeSignatures.md) to model shardable tensor
block algebra expressions.

See the [Loom Graph Semantics](LoomGraphSemantics.md) document for a detailed discussion of the
extensible graph representation and semantics.

An example intermediate static graph, showcasing block sharding:

<table cellborder="0">
  <tr>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="linear.relu" src="media/linear.relu.ortho.jpg"/>
      </div>
    </td>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="linear.relu.4x" src="media/linear.relu.4x.ortho.jpg"/>
      </div>
    </td>
  </tr>
</table>

## Development Philosophy

The **Tapestry** values are, in order of precedence:

1. **long term velocity** - the ability to add new features and fix bugs over the long term.
   - **clean apis** - every layer of the apis should be easy to understand and extend.
   - **clean tests** - the readability and extendability of the tests should be a priority.
   - **documentation** - all behavior should be documented.
2. **clean theory** - the mechanics being modeled should be consistent and correct.
   - **no escape hatches** - the behavior models should not have catch-all escape hatches.
3. **speed** - the algorithms should be fast.
4. **extensibility** - the ability to add new features.
   - **plugins** - as much behavior as possible should be implemented as plugins.
5. **usefulness** - the ability to solve real-world problems.

As the target for **Tapestry** is a high-throughput stochastic pareto-front optimizer; speed and
correctness are critically important; and that is why **Tapestry** is built in a compiled language.

However, **speed** and **correctness** are **products** of the ability of researchers and developers
to understand, verify, visualize, and extend the code.

**Usefulness** is the last on the list. This represents a focus on the long-term internal velocity
and correctness of tapestry over short-term feature velocity of applications.

## Sub-Projects

### Loom

[Tapestry Loom](TapestryLoom.md) is a graph representation and manipulation library for representing
and manipulating tensor block algebra expressions.

There are a number of [Loom Dialects](dialects/README.md) which are used to define the legal node
and tag types used in layers of the system, along with a collection of constraints and rules for the
graph.

### Weft

[Tapestry Weft](TapestryWeft.md) is a library for symbolic execution of metakernels to static block
operation graphs. It includes the libraries for authoring metakernels, and the development work on
template metakernels.

### ZSpace

[Tapestry ZSpace](TapestryZSpace.md) is a library for coordinate space range and projection api for
describing kernel index projections, as a GPU-independent Z-Space (integer) tensor library.
