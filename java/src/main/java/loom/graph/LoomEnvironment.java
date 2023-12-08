package loom.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import loom.common.json.JsonSchemaManager;
import loom.common.serialization.JsonUtil;

/**
 * Loom Graph Environment.
 *
 * <p>Describes the parsing and validation environment for a graph.
 */
@Data
@Builder
public final class LoomEnvironment {
  @FunctionalInterface
  public interface Constraint {
    void check(LoomEnvironment env, LoomGraph graph);
  }

  @Nonnull private final LoomGraph.NodeMetaFactory nodeMetaFactory;

  @Builder.Default private final JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();

  @Singular @Nonnull private final List<Constraint> constraints = new ArrayList<>();

  /**
   * Load a graph from a JSON string in this environment.
   *
   * @param json the JSON string.
   * @return the graph.
   */
  public LoomGraph graphFromJson(String json) {
    var tree = JsonUtil.parseToJsonNodeTree(json);

    var graph = LoomGraph.builder().env(this).build();

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

  public void validateGraph(LoomGraph graph) {
    for (var node : graph.getNodes().values()) {
      node.validate();
    }

    for (var constraint : constraints) {
      constraint.check(this, graph);
    }
  }

  /**
   * Create a new graph builder with this environment.
   *
   * @return the builder.
   */
  public LoomGraph.LoomGraphBuilder graphBuilder() {
    return LoomGraph.builder().env(this);
  }

  public LoomGraph createGraph() {
    return graphBuilder().id(UUID.randomUUID()).env(this).build();
  }
}
