package loom.doozer;

import lombok.Builder;
import lombok.Data;
import loom.common.serialization.JsonUtil;
import net.jimblackler.jsonschemafriend.SchemaStore;

import java.util.UUID;

/**
 * Loom Graph Environment.
 *
 * <p>Describes the parsing and validation environment for a graph.
 */
@Data
@Builder
public final class DoozerEnvironment {
  private final SchemaStore schemaStore = new SchemaStore(true);
  private final DoozerGraph.NodeMetaFactory nodeMetaFactory;

  /**
   * Load a graph from a JSON string in this environment.
   *
   * @param json the JSON string.
   * @return the graph.
   */
  public DoozerGraph graphFromJson(String json) {
    var tree = JsonUtil.parseToJsonNodeTree(json);

    var graph = DoozerGraph.builder().env(this).build();

    for (var entry : tree.properties()) {
      var key = entry.getKey();
      if (key.equals("id")) {
        graph.setId(UUID.fromString(entry.getValue().asText()));

      } else if (key.equals("nodes")) {
        for (var nodeTree : entry.getValue()) {
          var node = getNodeMetaFactory().nodeFromTree(nodeTree);
          graph.addNode(node);
        }
      } else {
        throw new IllegalArgumentException("Unknown property: " + key);
      }
    }

    return graph;
  }

  /**
   * Create a new graph builder with this environment.
   *
   * @return the builder.
   */
  public DoozerGraph.DoozerGraphBuilder graphBuilder() {
    return DoozerGraph.builder().env(this);
  }
}
