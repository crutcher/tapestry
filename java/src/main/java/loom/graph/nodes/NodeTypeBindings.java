package loom.graph.nodes;

import java.net.URI;
import java.util.List;
import lombok.Data;
import loom.common.serialization.JsonUtil;
import loom.graph.LoomGraph;
import loom.graph.LoomGraphEnv;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

/**
 * Node type operations.
 *
 * <p>Defines the type and field schema for a node type.
 */
@Data
public abstract class NodeTypeBindings {
  private final String type;
  private final String fieldSchema;

  /**
   * Check a node against the JSD schema for the type.
   *
   * <p>Collects any validation issues in the given collector.
   *
   * @param env the environment.
   * @param node the node to check.
   * @param issueCollector the issue collector.
   */
  public final void checkNodeSchema(
      LoomGraphEnv env, LoomGraph.NodeDom node, ValidationIssueCollector issueCollector) {
    env.getJsdManager()
        .applySchemaJson(
            issueCollector,
            URI.create("urn:loom:node:" + type),
            JsonUtil.parseToMap(fieldSchema),
            node.jpath(),
            JsonUtil.toJson(node.getFields()),
            List.of(
                ValidationIssue.Context.builder()
                    .name("Node")
                    .jsonpath(node.jpath())
                    .dataFromTree(node.getDoc())
                    .build(),
                ValidationIssue.Context.builder()
                    .name("Field Schema")
                    .jsonData(fieldSchema)
                    .build()));
  }

  /**
   * Check a node.
   *
   * <p>Collects any validation issues in the given collector.
   *
   * <p>Subclasses can override this method to perform additional checks.
   *
   * @param env the environment.
   * @param node the node to check.
   * @param issueCollector the issue collector.
   */
  public void checkNodeSemantics(
      LoomGraphEnv env, LoomGraph.NodeDom node, ValidationIssueCollector issueCollector) {}
}
