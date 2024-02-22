package org.tensortapestry.weft.metakernels;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;

@UtilityClass
public class CommonMetaKernels {

  public static final Set<String> DATA_TYPES = Set.of("int32", "int64", "float32", "float64");
  public final MetaKernel ADD = new CellWiseAccumulatorMetaKernel("op:add", DATA_TYPES);

  public final MetaKernel LINEAR = new LinearMetaKernel(DATA_TYPES);

  public final MetaKernel RELU = new ReluMetaKernel(DATA_TYPES);

  public static OperationNode importTensor(
    LoomGraph graph,
    TensorNode tensorNode
  ) {
    var outputs = Map.of(
      "result",
      List.of(tensorNode.getTensorSelection())
    );

    var op =
      OperationNode.on(graph)
        .body(b -> {
          b.kernel("loom:import");
          b.outputs(outputs);
        })
        .build();

    op.addAnnotation(
      TensorOpNodes.IO_SEQUENCE_POINT_TYPE,
      IOSequencePoint.builder().build()
    );

    var ap = ApplicationNode
      .on(graph)
      .body(b -> b.operationId(op.getId()).outputs(outputs))
      .build();

    return op;
  }

  public static OperationNode exportTensor(
    LoomGraph graph,
    TensorNode tensorNode
  ) {
    var inputs = Map.of(
      "tensor",
      List.of(tensorNode.getTensorSelection())
    );

    var op =
      OperationNode.on(graph)
        .body(b -> {
          b.kernel("loom:export");
          b.inputs(inputs);
        })
        .build();

    op.addAnnotation(
      TensorOpNodes.IO_SEQUENCE_POINT_TYPE,
      IOSequencePoint.builder().build()
    );

    var ap = ApplicationNode
      .on(graph)
      .body(b -> b.operationId(op.getId()).inputs(inputs))
      .build();

    return op;
  }
}
