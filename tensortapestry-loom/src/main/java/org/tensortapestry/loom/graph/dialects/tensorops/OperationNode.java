package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.common.collections.StreamableIterable;
import org.tensortapestry.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.AbstractNodeWrapper;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.common.JsdType;

@JsdType(OperationNode.TYPE)
public final class OperationNode extends AbstractNodeWrapper<OperationNode, OperationNode.Body> {

  public static final String TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/nodes/Operation";

  public static final class Builder
    extends AbstractNodeWrapperBuilder<OperationNode, Builder, Body, Body.BodyBuilder> {

    private Builder() {
      super(TYPE, Body::builder, Body.BodyBuilder::build, OperationNode::wrap);
    }
  }

  @Value
  @Jacksonized
  @lombok.Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({ "kernel", "params", "inputs", "outputs" })
  @JsdType(TYPE)
  public static class Body implements HasToJsonString {

    @Nonnull
    String kernel;

    @Singular
    Map<String, Object> params;

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
  public static Builder on(LoomGraph graph) {
    return new Builder().graph(graph);
  }

  @Nonnull
  public static OperationNode wrap(@Nonnull LoomNode node) {
    return new OperationNode(node);
  }

  public OperationNode(@Nonnull LoomNode node) {
    super(node.assertType(TYPE), Body.class);
  }

  @Delegate
  @SuppressWarnings("unused")
  private Body delegateBodyMethods() {
    return getBody();
  }

  public StreamableIterable<ApplicationNode> getApplicationNodes() {
    var id = getId();
    return () ->
      assertGraph()
        .byType(ApplicationNode.class)
        .stream()
        .filter(n -> n.getOperationId().equals(id))
        .iterator();
  }

  /**
   * Map the input selections to the actual TensorNodes in the graph.
   *
   * @return a map of output names to the TensorNodes.
   */
  public Map<String, List<TensorNode>> getInputNodes() {
    var g = assertGraph();
    return getInputs()
      .entrySet()
      .stream()
      .collect(
        Collectors.toUnmodifiableMap(
          Map.Entry::getKey,
          e ->
            e
              .getValue()
              .stream()
              .map(ts -> g.assertNode(ts.getTensorId(), TensorNode.class))
              .toList()
        )
      );
  }

  /**
   * Map the output selections to the actual TensorNodes in the graph.
   *
   * @return a map of output names to the TensorNodes.
   */
  public Map<String, List<TensorNode>> getOutputNodes() {
    var g = assertGraph();
    return getOutputs()
      .entrySet()
      .stream()
      .collect(
        Collectors.toUnmodifiableMap(
          Map.Entry::getKey,
          e ->
            e
              .getValue()
              .stream()
              .map(ts -> g.assertNode(ts.getTensorId(), TensorNode.class))
              .toList()
        )
      );
  }

  /**
   * Get the single result node from the operation.
   *
   * @return the result node.
   * @throws IllegalStateException if there is not exactly one output.
   */
  public TensorNode getResult() {
    var outputs = getOutputNodes();
    if (outputs.size() != 1) {
      throw new IllegalStateException("OperationNode has more than one output: " + getOutputs());
    }
    var nodes = outputs.values().stream().findFirst().orElseThrow();
    if (nodes.size() != 1) {
      throw new IllegalStateException("OperationNode has more than one output: " + getOutputs());
    }
    return nodes.getFirst();
  }
}
