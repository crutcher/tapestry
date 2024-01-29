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
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.common.JsdType;

public class ApplicationNode extends AbstractNodeWrapper<ApplicationNode.Body> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Application";

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

    @Nonnull
    UUID operationId;

    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> inputs;

    @Singular
    @Nonnull
    Map<String, List<TensorSelection>> outputs;
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  public static Builder builder(LoomGraph graph) {
    return new Builder().graph(graph);
  }

  @Nonnull
  public static ApplicationNode wrap(@Nonnull LoomNode node) {
    return new ApplicationNode(node);
  }

  public ApplicationNode(@Nonnull LoomNode node) {
    super(node.assertType(TYPE), Body.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private Body delegateBodyMethods() {
    return getBody();
  }

  public OperationNode getOperationNode() {
    return assertGraph().assertNode(OperationNode::wrap, getOperationId());
  }
}
