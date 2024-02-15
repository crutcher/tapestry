# Tensor Tapestry Compiler Suite

**Tapestry** is an experimental tensor expression optimizing compiler suite.

The goal of **Tapestry** is to provide an ecosystem for a high-performance stochastic pareto-front
optimizer for distributed tensor expressions, targeting optimizations which are permitted to search
for extended time on a large number of machines.

See the [Tapestry Documentation](docs/README.md) for more information.

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

An example intermediate static graph, showcasing block sharding:

<img src="tensortapestry-loom/docs/media/example.svg" width="800"/>

There is a [Tapestry Project Writeup](https://crutcher.github.io/Tapestry/) for a more detailed
overview (~100 pages) of the theory and goals of the project. Note that this document was written
before the current development project, and is more of a research direction plan and overview of
polyhedral optimization theory than a description of the current state of the project's
architecture.

## Sub-Projects

- **[loom](tensortapestry-loom/README.md)** - tensor expression graph representation.
- **[weft](tensortapestry-weft/README.md)** - metakernel symbolic execution api.
- **[ZSpace](tensortapestry-zspace/README.md)** - integer space (Z-space) tensor math library, with
  focus on index ranges and projection functions.

## Getting Started

In the current stage of development, **loom** produces no tool targets; and exists solely as a
collection of libraries and tests.

It **should** setup cleanly in any modern development environment; but full external dependencies
are not yet documented.

## Active Work Surfaces

- [Metakernel Template Language Design](https://github.com/crutcher/loom/issues/2)
- [Loom Type URI Schema](https://github.com/crutcher/loom/issues/3)
- [Launch Project Domain](https://github.com/crutcher/loom/issues/4)
- [Setup Maven Packages](https://github.com/crutcher/loom/issues/5)
