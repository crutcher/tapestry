package loom.graph.nodes;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomGraph;
import loom.zspace.ZPoint;

@Jacksonized
@SuperBuilder
public final class TensorNode extends LoomGraph.Node<TensorNode, TensorNode.Body> {
  @Data
  @Jacksonized
  @Builder
  public static class Body {
    @Nonnull private String dtype;

    @Nonnull private ZPoint shape;
  }

  @Override
  public Class<Body> getBodyClass() {
    return Body.class;
  }

  @Builder
  @Getter
  public static class Meta extends LoomGraph.NodeMeta<TensorNode, Body> {
    public static final String TYPE = "TreeNode";

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
    public void validateNode(TensorNode node) {
      if (!node.getShape().coords.isStrictlyPositive()) {
        throw new IllegalArgumentException(
            "shape must be positive and non-empty: " + node.getShape());
      }
    }
  }

  /** Exists to support {@code @Delegate} for {@code getBody()}. */
  @SuppressWarnings("unused")
  @Delegate
  private Body delegateProvider() {
    return getBody();
  }
}
