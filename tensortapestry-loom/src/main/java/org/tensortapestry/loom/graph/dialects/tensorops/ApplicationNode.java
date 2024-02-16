package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.common.JsdType;

/**
 * Node wrapper for an Application node.
 */
@JsdType(ApplicationNode.TYPE)
public class ApplicationNode extends AbstractNodeWrapper<ApplicationNode, ApplicationNode.Body> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/nodes/Application";

  /**
   * Builder for a ApplicationNode.
   */
  public static final class Builder
    extends AbstractNodeWrapperBuilder<ApplicationNode, Builder, Body, Body.BodyBuilder> {

    private Builder() {
      super(TYPE, Body::builder, Body.BodyBuilder::build, ApplicationNode::wrap);
    }
  }

  /**
   * The body of the ApplicationNode.
   */
  @Value
  @Jacksonized
  @lombok.Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({ "operationId", "inputs", "outputs" })
  @JsdType(TYPE)
  public static class Body implements HasToJsonString {

    /**
     * The ID of the operation node that this application node applies.
     */
    @Nonnull
    UUID operationId;

    /**
     * The inputs to the application.
     */
    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> inputs;

    /**
     * The outputs of the application.
     */
    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> outputs;
  }

  /**
   * Builder for a ApplicationNode.
   *
   * @return a new Builder.
   */
  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for an ApplicationNode.
   *
   * @param graph the graph to build the node for.
   * @return a new Builder.
   */
  @Nonnull
  public static Builder on(LoomGraph graph) {
    return new Builder().graph(graph);
  }

  /**
   * Wrap a LoomNode as an ApplicationNode.
   *
   * @param node the node to wrap.
   * @return the wrapped node.
   * @throws IllegalStateException if the node is not a ApplicationNode.
   */
  @Nonnull
  public static ApplicationNode wrap(@Nonnull LoomNode node) {
    return new ApplicationNode(node);
  }

  /**
   * Wrap a LoomNode as a ApplicationNode.
   *
   * @param node the node to wrap.
   * @throws IllegalStateException if the node is not a ApplicationNode.
   */
  public ApplicationNode(@Nonnull LoomNode node) {
    super(node.assertType(TYPE), Body.class);
  }

  /**
   * Private hook for {@code @Delegate}.
   *
   * @return the body of the node.
   */
  @Delegate
  @SuppressWarnings("unused")
  private Body delegateBodyMethods() {
    return getBody();
  }

  /**
   * Get the operation node that this application node applies.
   *
   * @return the operation node.
   */
  public OperationNode getOperationNode() {
    return assertGraph().assertNode(getOperationId(), OperationNode.class);
  }
}
