package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.common.JsdType;

/**
 * A node representing an application of an operation signature to a set of tensors.
 */
@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class ApplicationNode extends LoomNode<ApplicationNode, ApplicationNode.Body> {

  /**
   * Extensions to the ApplicationNodeBuilder.
   * @param <C> the concrete ApplicationNode type.
   * @param <B> the concrete ApplicationNodeBuilder type.
   */
  @SuppressWarnings("unused")
  public abstract static class ApplicationNodeBuilder<
    C extends ApplicationNode, B extends ApplicationNodeBuilder<C, B>
  >
    extends LoomNodeBuilder<ApplicationNode, Body, C, B> {
    {
      // Set the node type.
      type(TensorOpNodes.APPLICATION_NODE_TYPE);
    }
  }

  /**
   * The body of the ApplicationNode.
   */
  @Value
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({ "operationId", "inputs", "outputs" })
  @JsdType(TensorOpNodes.APPLICATION_NODE_TYPE)
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

  /**
   * Create an ApplicationNodeBuilder with the given body.
   *
   * <p>Example:
   *
   * <pre>{@code
   * var node = ApplicationNode.builder()
   *  .withBody(body -> body
   *    .input("x", List.of(
   *      new TensorSelection(tensor.getId(), tensor.getShape()))))
   *  .buildOnGraph(graph);
   * }</pre>
   *
   * @param cb the body builder callback.
   * @return the ApplicationNodeBuilder.
   */
  public static ApplicationNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = { HasToJsonString.class })
  @Nonnull
  private Body body;

  /**
   * Get the operation signature node.
   * @return the operation signature node.
   */
  public OperationSignatureNode getOperationSignatureNode() {
    return assertGraph()
      .assertNode(
        getOperationId(),
        TensorOpNodes.OPERATION_SIGNATURE_NODE_TYPE,
        OperationSignatureNode.class
      );
  }
}
