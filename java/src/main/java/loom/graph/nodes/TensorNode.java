package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.graph.LoomConstants;
import loom.graph.LoomGraph;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.HasDimension;
import loom.zspace.HasSize;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.apache.commons.lang3.builder.HashCodeExclude;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
              }
            },
            "origin": {
              "documentation": "The origin is optional, and defaults to zeros. The dimensions of origin must match the dimensions of shape.",
              "type": "array",
              "items": {
                "type": "integer"
              }
            }
          },
          "required": ["dtype", "shape"]
      }
      """;

  public interface HasValidate {
    void validate(ValidationIssueCollector issueCollector);
  }

  @Data
  @ToString(onlyExplicitlyIncluded = true)
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static final class Body implements HasValidate, HasDimension, HasToJsonString, HasSize {
    public static class BodyBuilder {
      public Body build() {
        var body = new Body(dtype, shape, origin);
        var collector = new ListValidationIssueCollector();
        body.validate(collector);
        collector.check();
        return body;
      }
    }

    @ToString.Include @Nonnull private final String dtype;

    @ToString.Include @Nonnull private final ZPoint shape;

    @ToString.Include @Nullable private final ZPoint origin;

    @Override
    public int getNDim() {
      return shape.getNDim();
    }

    @Override
    public int getSize() {
      return getEffectiveRange().getSize();
    }

    @HashCodeExclude
    @Getter(lazy = true)
    @JsonIgnore
    private final ZPoint effectiveOrigin = computeEffectiveOrigin();

    @HashCodeExclude
    @Getter(lazy = true)
    @JsonIgnore
    private final ZRange effectiveRange = computeEffectiveRange();

    private ZPoint computeEffectiveOrigin() {
      return origin != null ? origin : ZPoint.newZerosLike(getShape());
    }

    private ZRange computeEffectiveRange() {
      return ZRange.fromStartWithShape(getEffectiveOrigin(), getShape());
    }

    @Override
    public void validate(ValidationIssueCollector issueCollector) {
      if (!getShape().coords.isStrictlyPositive()) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .summary("shape must be positive and non-empty: %s".formatted(getShape()))
                .build());
      }

      if (getOrigin() != null && getOrigin().getNDim() != getShape().getNDim()) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .summary(
                    "origin %s dimensions != shape %s dimensions"
                        .formatted(getOrigin(), getShape()))
                .build());
      }
    }
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
      node.getBody().validate(issueCollector);
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

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private Body body;
}
