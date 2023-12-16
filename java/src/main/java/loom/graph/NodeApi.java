package loom.graph;

import loom.graph.nodes.OperationNode;
import loom.graph.nodes.TensorNode;
import loom.zspace.ZPoint;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeApi {
  private NodeApi() {}

  public static TensorNode newTensor(LoomGraph graph, String dtype, ZPoint shape) {
    return graph.addNode(TensorNode.builder(TensorNode.Body.builder().dtype(dtype).shape(shape)));
  }

  public static OperationNode newOperation(
      LoomGraph graph,
      String opName,
      Map<String, List<TensorNode>> inputs,
      Map<String, List<TensorNode>> outputs,
      @Nullable Map<String, Object> params) {
    params = params == null ? new HashMap<>() : new HashMap<>(params);
    return graph.addNode(
        OperationNode.builder(
            OperationNode.Body.builder()
                .opName(opName)
                .inputs(OperationNode.nodeMapToIdMap(inputs))
                .outputs(OperationNode.nodeMapToIdMap(outputs))
                .params(params)));
  }
}
