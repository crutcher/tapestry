package loom.doozer.nodes;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.doozer.DoozerGraph;
import loom.zspace.ZPoint;

@Jacksonized
@SuperBuilder
public final class TensorNode extends DoozerGraph.Node<TensorNode, TensorNode.Body> {
  @Data
  @Jacksonized
  @Builder
  public static class Body {
    @Nonnull private String dtype;

    @Nonnull private ZPoint shape;
  }

  public static class Meta extends NodeMeta<TensorNode, Body> {
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

    public Meta() {
      super(TensorNode.class, Body.class, BODY_SCHEMA);
    }

    @Override
    public void validateBody(Body body) {
      if (!body.getShape().coords.isStrictlyPositive()) {
        throw new IllegalArgumentException(
            "shape must be positive and non-empty: " + body.getShape());
      }
    }
  }

  /** Exists to support {@code @Delegate} for {@code getBody()}. */
  @Delegate
  private Body delegateProvider() {
    return getBody();
  }
}
