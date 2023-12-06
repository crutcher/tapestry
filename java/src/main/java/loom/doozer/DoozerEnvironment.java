package loom.doozer;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import loom.common.serialization.JsonUtil;
import net.jimblackler.jsonschemafriend.SchemaStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loom Graph Environment.
 *
 * <p>Describes the parsing and validation environment for a graph.
 */
@Data
@Builder
public final class DoozerEnvironment {
  @FunctionalInterface
  public interface Constraint {
    void check(DoozerEnvironment env, DoozerGraph graph);
  }

  @Nonnull private final DoozerGraph.NodeMetaFactory nodeMetaFactory;

  @Builder.Default private final SchemaStore schemaStore = new SchemaStore(true);

  @Singular @Nonnull private final List<Constraint> constraints = new ArrayList<>();

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

  public void validateGraph(DoozerGraph graph) {
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
  public DoozerGraph.DoozerGraphBuilder graphBuilder() {
    return DoozerGraph.builder().env(this);
  }
}
