# Tapestry Tensor Expression Compiler Suite

<b>"It's Just One Ocean" -Crutcher</b>

## Overview

**Tapestry** is an experimental tensor expression compiler framework.

Modern GPU-filled datacenters contain thousands of nodes with 8+ GPUs each, and are capable of
performing trillions of floating point operations per second. The goal of **Tapestry** is to unlock
the full potential of modern GPU-filled datacenters, by providing a foundational programming
environment for scalable, optimized, massively multi-GPU tensor programs.

Tensor programs underlie all deep-network based AI, and all finite element numerical simulations.
These include numerical weather and fluid simulations, protein folding and drug discovery
simulations, quantum chemistry simulations, financial simulations, and material design and
manufacturing simulations. Modern tensor programming environments are designed to maximize
productivity of developers working on single-GPU workstations, and struggle to express programs
which can be scheduled across even a few GPUs. Some of these frameworks do have solutions to scaling
up limited workloads, but no general-purpose solutions exist for scaling up arbitrary tensor
programs.

Multiple existing companies operate with >$1B/year annual hardware budgets for these simulations,
somewhere in the dozens of $B/year are being spent worldwide on these calculations today.

Though it is difficult to predict in advance the speedups of a new optimizing compiler, it is the
case that, due to the semantics of their programming models, the vast majority of existing tensor
applications are run with no meaningful structural optimizations; the programs are run directly as
human engineers have written them, with no further optimizations. This is akin to directly executing
a SQL query without any query planner or optimizer. The potential wins in efficiency for existing
applications from an optimizing compiler are therefore large; conservatively in the 30% range; but
for some applications, the potential is dramatically larger.

Irrespective of efficiency wins, the potential for new applications is tremendous; existing
applications are limited by the interconnect scheduling and manual design of the programs, and
removing these limitations will enable new applications which are not possible today.

At the current time, **Tapestry** is sitting upon years of development towards a solid theoretical
foundation, of shardable, composable, and re-writable polyhedral model tensor block algebra
operation expressions on an extensible compiler framework. The work is focused on exploiting this
mathematical foundation towards a practical compiler suite. Expectations are that the project needs
1-3 aggregate engineer-years of work to reach a state where it can be used to compile real-world
applications.

This is a big-pull project; the payoffs are huge, but the work required to climb from theory back to
practical parity with existing frameworks is substantial. There are many opportunities for
development applications along the way, empowered by that solid theoretical foundation. We are
seeking contributors, reviewers, and enthusiasts to help bring this project to life sooner. Funding
support, or safe-harbor in a larger organization, would also be very helpful.

<img style="width: 20%" alt="linear.relu.4x" src="docs/media/linear.relu.4x.ortho.jpg"/>

## Getting Started

### Basics

```bash
$ sudo apt-get install maven openjdk-21-jdk
$ ./mvnw verify
```

### Read the Documentation

The full [Tapestry Documentation](docs/README.md) provides much more detailed information about the
project's motivation, goals, design, and implementation.

### Join the Discord

[![Banner](https://invidget.switchblade.xyz/PNpSrFMeUb?theme=light)](https://discord.gg/PNpSrFMeUb)

If you have any interest in the project, please join the Discord server. We are actively looking for
reviewers, contributors, fans, theorists, and developers and would love to have you involved.

A huge portion of bringing this project to life is building a community of enthusiasts and experts
who can help guide the project, not only through theory and code; but also through iterative
development of the documentation, making the project accessible to wider audiences.

We are particularly interested in:

- document reviewers
- project managers
- programmers
- compiler theorists

### File an Issue / Bug

We are actively looking for feedback on the project. If you have any issues, please file a bug on
the [Issues](https://github.com/crutcher/tapestry/issues) page.

### Join the Discussions

If you have longer-form concerns to discuss, please post them in the project
[Discussions](https://github.com/crutcher/loom/discussions) board.

## Setup / Contributing Code

If you are interested in running the existing test suites, or contributing code, you'll need to
clone the repository and setup the development environment.

The project is a JDK 21 multi-module Maven/Java project, and should be setup in any modern
development IDE (JetBrains, VSC, etc).

That said, the project has been developed by one person thus far, and may have some missing
dependencies or undocumented requirements. If you run into any issues, please join the Discord or
file a bug (or both!) with as much information as possible, and I'll prioritize fixing the cause or
documenting the missing dependency.
