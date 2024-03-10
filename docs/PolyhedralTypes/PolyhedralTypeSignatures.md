# Polyhedral Type Signatures

This document describes the semantics of *Polyhedral Type Signatures* as used in *Tapestry*.

## Background

The [Polyhedral / Polytope Model](https://en.wikipedia.org/wiki/Polyhedral_model) is a mathematical
model for representing and reasoning about finite index sets and their associated iteration spaces.

## Introduction

A **Tensor Block Expression Operation** is a tensor expression operation which
defines some number of tensor outputs in terms of some number of tensor inputs;
in a form which can be sharded and distributed.

<table style="border: 0">
  <tr>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="ops" src="PolyhedralTypeSignatures/Operator.fig1.dot.png"/>
      </div>
    </td>
    <td>
      <div style="width: 100%; margin: auto">
        <img alt="sharded ops" src="PolyhedralTypeSignatures/Operator.fig2.dot.png"/>
      </div>
    </td>
  </tr>
</table>

A **Polyhedral Type Signature** defines the semantics by which an annotated tensor block
expression operation can be sharded and distributed.


