package loom.graph.nodes;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.*;
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
@Getter
@Setter
public final class TensorNode extends LoomGraph.Node<TensorNode, TensorNode.Body> {
  @Delegate @Nonnull private Body body;

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Prototype#TYPE}.
   *
   * @param body the body to use.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(Body body) {
    return builder().body(body);
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Prototype#TYPE}.
   *
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder() {
    return new TensorNodeBuilderImpl();
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Prototype#TYPE}.
   *
   * @param body the body to use.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(Body.BodyBuilder body) {
    return builder().body(body.build());
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Prototype#TYPE}.
   *
   * @param dtype the dtype.
   * @param shape the shape.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(String dtype, ZPoint shape) {
    return builder().body(Body.builder().dtype(dtype).shape(shape).build());
  }

  @Data
  @Jacksonized
  @Builder
  public static final class Body {
    @Nonnull private String dtype;

    @Nonnull private ZPoint shape;
  }

  public static final class TensorNodeBuilderImpl
      extends TensorNodeBuilder<TensorNode, TensorNodeBuilderImpl> {
    {
      prototype(Prototype.builder().build());
      type(Prototype.TYPE);
    }
  }

  @Builder
  @Getter
  public static final class Prototype extends LoomGraph.NodePrototype<TensorNode, Body> {
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
    public Prototype(Set<String> validDTypes) {
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
    public Prototype addValidDType(String validDType) {
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
}
