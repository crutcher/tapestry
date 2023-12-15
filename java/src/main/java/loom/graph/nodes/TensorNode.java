package loom.graph.nodes;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomConstants;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;

@Jacksonized
@SuperBuilder
public final class TensorNode extends LoomGraph.Node<TensorNode, TensorNode.Body> {

  @Data
  @Jacksonized
  @Builder
  public static final class Body {
    @Nonnull private String dtype;

    @Nonnull private ZPoint shape;
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder() {
    return new TensorNodeBuilderImpl().type(Meta.TYPE);
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @param body the body to use.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(Body body) {
    return builder().body(body);
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @param body the body to use.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(Body.BodyBuilder body) {
    return builder().body(body.build());
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @param dtype the dtype.
   * @param shape the shape.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(String dtype, ZPoint shape) {
    return builder().body(Body.builder().dtype(dtype).shape(shape).build());
  }

  @Override
  public Class<Body> getBodyClass() {
    return Body.class;
  }

  @Builder
  @Getter
  public static final class Meta extends LoomGraph.NodeMeta<TensorNode, Body> {
    public static final String TYPE = "TensorNode";

    public static final String BODY_SCHEMA =
        """
        {
            "type": "object",
            "properties": {
              "dtype": {
                  "type": "string"
              },
              "shape": {
                  "type": "array",
                  "items": {
                    "type": "integer",
                    "minimum": 1
                  },
                  "minItems": 1
                }
              },
            "required": ["dtype", "shape"]
        }
        """;

    @Singular private final Set<String> validDTypes;

    @Builder
    public Meta(Set<String> validDTypes) {
      super(TensorNode.class, Body.class, BODY_SCHEMA);
      this.validDTypes = new HashSet<>(validDTypes);
    }

    /**
     * Add a valid dtype.
     *
     * @param validDType the valid dtype.
     * @return this Meta, for chaining.
     */
    @CanIgnoreReturnValue
    public Meta addValidDType(String validDType) {
      validDTypes.add(validDType);
      return this;
    }

    @Override
    public void validateNode(TensorNode node, ValidationIssueCollector issueCollector) {
      if (!node.getShape().coords.allMatch(x -> x > 0)) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .message("shape must be positive and non-empty")
                .build());
      }
    }
  }

  /** Exists to support {@code @Delegate} for {@code getBody()}. */
  @SuppressWarnings("unused")
  @Delegate
  private Body delegateProvider() {
    return getBody();
  }

  /**
   * Get the operation node that produces this tensor.
   *
   * @return the operation node.
   */
  public OperationNode getSourceOperationNode() {
    // This assumes that there is only one source operation node.
    var id = getId();
    return assertGraph().stream(OperationNode.Meta.TYPE, OperationNode.class)
        .filter(op -> op.getOutputs().values().stream().anyMatch(ids -> ids.contains(id)))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Get the operation node ID that produces this tensor.
   *
   * @return the operation node ID.
   */
  public UUID getSourceOperationId() {
    return getSourceOperationNode().getId();
  }
}
