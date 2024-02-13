# Tapestry Loom

**Loom** is a **Tapestry** module which provides extensible tensor expression graph representation.

![example](docs/media/example.svg)

Core Classes:

- [LoomGraph](src/main/java/org/tensortapestry/loom/graph/LoomGraph.java): A holder for nodes.
- [LoomNode](src/main/java/org/tensortapestry/loom/graph/LoomNode.java): A node in the graph.
- [LoomEnvironment](src/main/java/org/tensortapestry/loom/graph/LoomEnvironment.java): Defines the
  semantics and validation for a _LoomGraph_.

There are various dialects which extend the _LoomEnvironment_ to provide additional semantics and
validation.

## Serialization

**Loom** is tightly coupled with the Jackson JSON serialization library; and most **Loom** objects
can be serialized to and deserialized from JSON.
