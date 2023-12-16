package loom.graph;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import org.junit.Test;

@SuppressWarnings("unused")
public class NodeFactoryTest extends BaseTestClass {
  @Data
  @SuperBuilder
  @AllArgsConstructor
  public abstract static class Node<NodeType extends Node<NodeType, BodyType>, BodyType> {
    private final NodePrototype<NodeType, BodyType> prototype;

    private final UUID id;
    private final String type;
    private String label;
    private BodyType body;
  }

  public static class NodePrototype<NodeType extends Node<NodeType, BodyType>, BodyType> {}

  @SuperBuilder
  public static final class TensorNode extends Node<TensorNode, TensorNode.Body> {
    public static final String TYPE = "TensorNode";

    @Builder
    public static final class Body {
      private final String dtype;
      private final ZPoint shape;
    }

    public static final class TensorNodeBuilderImpl
        extends TensorNodeBuilder<TensorNode, TensorNodeBuilderImpl> {
      {
        @SuppressWarnings("unused")
        var res = type(TYPE);
      }
    }
  }

  public static final class TensorNodePrototype extends NodePrototype<TensorNode, TensorNode.Body> {

    public TensorNode.TensorNodeBuilder<?, ?> builder() {
      return TensorNode.builder().prototype(this);
    }
  }

  @Test
  public void test() {
    var tensorNodePrototype = new TensorNodePrototype();

    @SuppressWarnings("unused")
    var node =
        tensorNodePrototype
            .builder()
            .id(UUID.randomUUID())
            .label("foo")
            .body(TensorNode.Body.builder().dtype("float32").shape(new ZPoint(1, 2, 3)).build())
            .build();
  }
}
