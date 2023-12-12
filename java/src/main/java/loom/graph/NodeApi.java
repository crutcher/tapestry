package loom.graph;

import java.util.List;
import java.util.Map;
import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.zspace.ZPoint;

public class NodeApi {
  private NodeApi() {}

  public static TensorNode newTensor(LoomGraph graph, String dtype, ZPoint shape) {
    return graph.addNode(TensorNode.builder(dtype, shape));
  }

  public static OperationNode newOperation(
      LoomGraph graph,
      String opName,
      Map<String, List<TensorNode>> inputs,
      Map<String, List<TensorNode>> outputs) {
    return graph.addNode(
        OperationNode.builder(
            OperationNode.Body.builder()
                .opName(opName)
                .inputs(OperationNode.nodeMapToIdMap(inputs))
                .outputs(OperationNode.nodeMapToIdMap(outputs))));
  }
}
