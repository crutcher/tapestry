package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.lazy.LazyString;
import loom.common.lazy.Thunk;
import loom.graph.LoomConstants;
import loom.graph.LoomGraph;
import loom.graph.WithSchema;
import loom.validation.HasValidate;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.HasDimension;
import loom.zspace.HasSize;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.apache.commons.lang3.builder.HashCodeExclude;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class TensorNode extends LoomGraph.Node<TensorNode, TensorNode.Body> {
  public static final String TYPE = "TensorNode";

  public abstract static class TensorNodeBuilder<
          C extends TensorNode, B extends TensorNodeBuilder<C, B>>
      extends NodeBuilder<TensorNode, Body, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  @Data
  @ToString(onlyExplicitlyIncluded = true)
  @Jacksonized
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @WithSchema(
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
  """)
  public static final class Body
      implements HasValidate<Body>, HasDimension, HasToJsonString, HasSize {
    public static class BodyBuilder {
      public Body build() {
        return new Body(dtype, shape, origin).checkValid();
      }
    }

    @ToString.Include @Nonnull private final String dtype;
    @ToString.Include @Nonnull private final ZPoint shape;
    @ToString.Include @Nullable private final ZPoint origin;

    @Override
    public int getNDim() {
      return getShape().getNDim();
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
    public void validate(
        @Nullable LazyString jsonPathPrefix,
        ValidationIssueCollector issueCollector,
        @Nullable Supplier<List<ValidationIssue.Context>> contextSupplier) {
      var lazyContext =
          Thunk.of(
              () ->
                  ValidationIssue.Context.builder()
                      .name("Body")
                      .jsonpath(jsonPathPrefix, "body")
                      .data(this)
                      .build());

      if (!getShape().coords.isStrictlyPositive()) {
        issueCollector.addIssue(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .summary("shape must be positive and non-empty: %s".formatted(getShape()))
                .context(lazyContext)
                .withContexts(contextSupplier)
                .build());
      }

      if (getOrigin() != null && getOrigin().getNDim() != getShape().getNDim()) {
        issueCollector.addIssue(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .summary(
                    "origin %s dimensions != shape %s dimensions"
                        .formatted(getOrigin(), getShape()))
                .context(lazyContext)
                .withContexts(contextSupplier)
                .build());
      }
    }
  }

  @Builder
  public static final class Prototype extends LoomGraph.NodePrototype<TensorNode, Body> {

    @Builder
    public Prototype() {
      super(TensorNode.class, Body.class);
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
