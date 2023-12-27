package loom.graph.nodes;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomGraph;

@Jacksonized
@SuperBuilder
@Getter
@Setter
public class PolyhedralSignatureNode
    extends LoomGraph.Node<PolyhedralSignatureNode, PolyhedralSignatureNode.Body> {

  public static final String TYPE = "PolyhedralSignatureNode";

  public static final String BODY_SCHEMA =
      """
        {
            "type": "object",
            "properties": {
              "inputs": {
                  "documentation": "Input tensors",
                  "$ref": "#/definitions/io_map"
              }
            },
            "required": ["inputs"],
            "definitions": {
              "io_map": {
                  "type": "object",
                  "patternProperties": {
                      "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                          "type": "array",
                          "items": {
                              "$ref": "#/definitions/index_projection"
                          },
                          "minItems": 1
                      }
                  },
                  "additionalProperties": false
              },
              "index_projection": {
                "type": "object",
                "additionalProperties": false,
              }
            }
        }
        """;

  @Data
  @Jacksonized
  @Builder
  public static final class Body {
    private String name;
    private String signature;
  }

  public abstract static class PolyhedralSignatureNodeBuilder<
          C extends PolyhedralSignatureNode, B extends PolyhedralSignatureNodeBuilder<C, B>>
      extends NodeBuilder<PolyhedralSignatureNode, Body, C, B> {
    {
      type(TYPE);
    }
  }

  public static final class Prototype
      extends LoomGraph.NodePrototype<PolyhedralSignatureNode, Body> {
    public Prototype() {
      super(PolyhedralSignatureNode.class, Body.class, BODY_SCHEMA);
    }
  }

  public static PolyhedralSignatureNodeBuilder<?, ?> withBody(Consumer<Body.BodyBuilder> cb) {
    var bodyBuilder = Body.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate @Nonnull private Body body;
}
