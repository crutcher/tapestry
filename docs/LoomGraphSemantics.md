# Loom Graph Semantics

[Tapestry Documentation](../README.md)

## Overview

**Tapestry Loom** is the **Tapestry** sub-project providing extensible graph representation,
scanning and manipulation api, and validation.

In the development of **Tapestry**, I explored 30+ apis for how to serialize the graph, how to
design the api for graph manipulation, how to validate that a given graph instance is well-formed
and meets a set of semantic constraints; and how to do the above in a way which alternative but
related graph languages can be easily defined via plugins.

To have a hope of strong portable tooling in the future, as much of the serialization and
manipulation api as possible must be defined in a way that is cheap to port exact copies of the api
to other languages and platforms.

The **Loom** project is the result of this exploration.

## Graph Representation

See:

- [LoomGraph.java](../tensortapestry-loom/src/main/java/org/tensortapestry/loom/graph/LoomGraph.java)
- [LoomNode.java](../tensortapestry-loom/src/main/java/org/tensortapestry/loom/graph/LoomNode.java)
- [LoomEnvironment.java](../tensortapestry-loom/src/main/java/org/tensortapestry/loom/graph/LoomEnvironment.java)

A **LoomGraph** is a collection of nodes, paired with a **LoomEnvironment** defining constraints on
what constitutes legal values and relationships for those nodes.

A raw **LoomGraph** is a JSON document collection of nodes:

```json
{
  "id": <UUID>,
  "nodes": [
    {
      "id": <UUID>,
      "label": <string>,
      "type": <string>,
      "body": <JSON>,
      "tags": {
        "<type>": <JSON>
      }
    },
    ...
  ]
}
```

Each node has:

- `id` - a unique UUID identifier
- `label` - an optional, non-unique string label
- `type` - a string type identifier
- `body` - a `type`-dependent JSON structure
- `tags` - a tag type keyed map of `{<type>: <JSON>}` of tag type dependent node extensions

Each **node type** and **tag type** is expected to have a corresponding schema, defined and enforced
by the **LoomEnvironment**; and is expected to be parsable by type-dependent node wrappers, which
understand the data and can provide a type-safe api for manipulating the data.

By defining additional types and constraints, we can compositionally construct language dialects
with strict semantics, reusing types and constraints across several dialects.
