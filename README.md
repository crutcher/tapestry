# Tensor Tapestry Compiler Suite

See the full [Tapestry Documentation](docs/README.md) for detailed information.

![Discord Banner 3](https://discordapp.com/api/guilds/1216844850104635484/widget.png?style=banner3)

**Tapestry** is an experimental tensor expression optimizing compiler suite.

It exists to make it easy to optimize applications (such as AI) to maximally exploit both
datacenters full of GPUs, and integrated FPGA stacks.

The goal of **Tapestry** is to provide an ecosystem for a high-performance stochastic pareto-front
optimizer for distributed tensor expressions, targeting optimizations which are permitted to search
for extended time on a large number of machines.

Here are examples showing a **Linear**/**ReLU** pipeline, with and without sub-block sharding;
demonstrating the potential for sub-shard operation fusion:

<table cellborder="0">
  <tr>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="linear.relu" src="docs/media/linear.relu.ortho.jpg"/>
      </div>
    </td>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="linear.relu.4x" src="docs/media/linear.relu.4x.ortho.jpg"/>
      </div>
    </td>
  </tr>
</table>

## Contributing

I'm actively looking for contributors to help with building or reviewing the project.

If you'd like to get involved, please post any questions in the project
[Discussions](https://github.com/crutcher/loom/discussions) board, or open an issue.

We could create a Discord server; if we got enough traction.

I'm particularly interested in contributors with experience in the following areas:

- maven lifecycle / package publishing
- technical documentation / editing
- compiler design
- tensor algebra
- optimization theory
- graph transformations
- graph representation
- distributed computing
- graph visualization

## Getting Started

In the current stage of development, **loom** produces no tool targets; and exists solely as a
collection of libraries and tests.

It **should** setup cleanly in any modern development environment; but full external dependencies
are not yet documented.

Documenting missing dependencies is a high priority and setup instructions is another high priority
which contributors could help with.
