package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.experimental.Delegate;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.NodeWrapper;

@UtilityClass
public class TensorOpNodes {

  // TODO: Switch to lookup by anchor when this is fixed:
  // See: https://github.com/networknt/json-schema-validator/pull/930

  public final String TENSOR_NODE_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Tensor";

  public LoomNode.LoomNodeBuilder tensorBuilder(
    LoomGraph graph,
    Consumer<TensorBody.TensorBodyBuilder> config
  ) {
    var bodyBuilder = TensorBody.builder();
    config.accept(bodyBuilder);
    return graph.nodeBuilder(TENSOR_NODE_TYPE).body(bodyBuilder.build());
  }

  public final String APPLICATION_NODE_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Application";

  public LoomNode.LoomNodeBuilder applicationBuilder(
    LoomGraph graph,
    Consumer<ApplicationBody.ApplicationBodyBuilder> config
  ) {
    var bodyBuilder = ApplicationBody.builder();
    config.accept(bodyBuilder);
    return graph.nodeBuilder(APPLICATION_NODE_TYPE).body(bodyBuilder.build());
  }

  public final String OPERATION_NODE_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Operation";

  public LoomNode.LoomNodeBuilder operationBuilder(
    LoomGraph graph,
    Consumer<OperationBody.OperationBodyBuilder> config
  ) {
    var bodyBuilder = OperationBody.builder();
    config.accept(bodyBuilder);
    return graph.nodeBuilder(OPERATION_NODE_TYPE).body(bodyBuilder.build());
  }

  public final String IPF_SIGNATURE_ANNOTATION_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd#/$defs/IPFSignature";
  public final String IPF_INDEX_ANNOTATION_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd#/$defs/IPFIndex";

  public static class TensorWrapper implements NodeWrapper {

    public static TensorWrapper wrap(LoomNode node) {
      return new TensorWrapper(node);
    }

    @Override
    public String toString() {
      return loomNode.toString();
    }

    @Delegate
    final LoomNode loomNode;

    public TensorWrapper(LoomNode loomNode) {
      this.loomNode = loomNode.assertType(TENSOR_NODE_TYPE);
    }

    @Override
    @Nonnull
    public LoomNode unwrap() {
      return loomNode;
    }

    @Delegate
    public TensorBody getBody() {
      return loomNode.getBody().viewAs(TensorBody.class);
    }
  }

  public static class OperationWrapper implements NodeWrapper {

    public static OperationWrapper wrap(LoomNode node) {
      return new OperationWrapper(node);
    }

    @Override
    public String toString() {
      return loomNode.toString();
    }

    @Delegate
    final LoomNode loomNode;

    public OperationWrapper(LoomNode loomNode) {
      this.loomNode = loomNode.assertType(OPERATION_NODE_TYPE);
    }

    @Override
    @Nonnull
    public LoomNode unwrap() {
      return loomNode;
    }

    @Delegate
    public OperationBody getBody() {
      return loomNode.getBody().viewAs(OperationBody.class);
    }
  }
}
