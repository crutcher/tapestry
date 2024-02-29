# Tapestry Prospectus

Hello, My name is Crutcher Dunnavant <crutcher+tapestry@gmail.com>

I am building a ground-up polyhedral type tensor block algebra expression optimizing compiler
toolchain. I am several years in, and I am looking for reviewers, collaborators, and funding
structures to accelerate development. Please forward to anyone you think should see this.

The primary project lives here: https://github.com/crutcher/tapestry

I have a high-level project overview document:
https://github.com/crutcher/tapestry/blob/main/docs/TapestryProjectOverview.md

<table style="border: 0">
  <tr>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="linear.relu" src="https://raw.githubusercontent.com/crutcher/tapestry/main/docs/media/linear.relu.ortho.jpg"/>
      </div>
    </td>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="linear.relu.4x" src="https://raw.githubusercontent.com/crutcher/tapestry/main/docs/media/linear.relu.4x.ortho.jpg"/>
      </div>
    </td>
  </tr>
</table>

I am deeply invested in the tensor expression optimization / distributed tensor application problem
space; I've spent years on it.

I spent time at Red Hat early in my career, then 10 years at Google. I built the internal
development filesystem CITC; and I worked in AI under Kurzweil. I've done a few startups.

One of the startups, 3Scan, focused on collecting, storing, and processing massive single-source
datasets from scanned tissue. Our datasets ranged from 10 to 50TB as _single_ 4D tensors (3D + color
channels). I spent a great deal of time working through the details needed to schedule efficient and
performant coherent kernel convolutions over logical tensors of that size. I designed and built a
polyhedral/polytope model scheduling engine for distributed tensor operations in that role; and we
were able to process 40TB tensors through 18 stage transformation pipelines in ~20 minutes, in 2018,
on CPUs.

Fast-forward a bit, and I expected to see PyTorch, Tensorflow, and Jax really embrace whole
expression optimization and polyhedral type signature manipulation, and they have not. The semantics
are too big a leap for backwards compatibility with their existing stacks, and it takes a long time
to work out the representation and optimization theory in the first place.

LLVM MLIR is exploring some of these ideas, as is ONNX; but both exclusively locally, and for highly
targeted execution environments.

I believe that a healthy ecosystem for tensor expressions would look like what we see for SQL, or
for Apache Spark/Apache Beam. Symbolic applications, written in a polyhedral type tensor block
algebra, with extremely strict semantics (and thus, visualizers, debuggers, etc, etc); with nice
category / functional programming theoretic combinators and widgets like we see in Haskell
libraries; coupled with a aggressive query rewrite/planner/scheduler targeting different execution
environments - local CPU, local GPU, distributed GPU, FPGA, etc.

I spent a long time exploring the implications of the polyhedral type theory:
https://github.com/crutcher/tapestry/blob/main/docs/PolyhedralTypesAndIndexProjection.md

I spent a long time exploring ~30 different IR representations; seaking one which preserved
modularity (the ability to extend the IR) AND constraint strictness (the ability to force semantics
on a particular dialect) AND type-safety for code execution. I found a least-bad answer, which I
believe is maximally portable to other languages.

The goal here is deep and aggressive search over the optimization surface; so working out a family
of primitives which does not damage the power of that optimization search, while still permitting an
api _similar_ to the ones existing users have come to expect has been a fair amount of work.

Everything in this iteration is written in Java. Pareto optimization search is embarrassingly
parallel, and as the goal is long-duration broad search of execution histories; a compiled language
makes the most out of compute resources.

Java costs ~2x Rust/C++ in performance; at a tremendously lower (~5x?) development cost. A completed
demonstration compiler could be ported to other languages; my goal is Rust, TypeScript, and possibly
C#; and library binding for other languages.

I am looking for collaborators, reviewers, and funding structures to accelerate development. I'm
particularly interested in contributors with experience in the following areas:

- maven lifecycle / package publishing
- technical documentation / editing
- compiler design
- tensor algebra
- optimization theory
- graph transformations
- graph representation
- distributed computing
- graph visualization
