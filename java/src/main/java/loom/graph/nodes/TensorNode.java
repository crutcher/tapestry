package loom.graph.nodes;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomConstants;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class TensorNode extends LoomGraph.Node<TensorNode, TensorNode.Body> {
  public static final String TYPE = "TensorNode";

  // TODO: body schema attached to Body, TensorNode via annotation links?
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

  @Data
  @Jacksonized
  @Builder
  public static final class Body {

    @Nonnull private String dtype;

    @Nonnull private ZPoint shape;
  }

  public abstract static class TensorNodeBuilder<
          C extends TensorNode, B extends TensorNodeBuilder<C, B>>
      extends NodeBuilder<TensorNode, Body, C, B> {
    {
      type(TYPE);
    }
  }

  @Builder
  @Getter
  public static final class Prototype extends LoomGraph.NodePrototype<TensorNode, Body> {

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
      if (!validDTypes.contains(node.getDtype())) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .summary("dtype (%s) must be one of %s".formatted(node.getDtype(), validDTypes))
                .build());
      }
      if (!node.getShape().coords.isStrictlyPositive()) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .summary("shape must be positive and non-empty: %s".formatted(node.getShape()))
                .build());
      }
    }
  }

  public static TensorNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  /* TODO: Node template BodyBuilderType, reflection annotation?
  * permits withBody() family to be base class methods.
  public abstract static BodyBuilderType bodyBuilder();
   */

  @Delegate @Nonnull private Body body;
}
