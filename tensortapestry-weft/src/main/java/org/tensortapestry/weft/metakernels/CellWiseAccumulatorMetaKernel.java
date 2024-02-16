package org.tensortapestry.weft.metakernels;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZAffineMap;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;
import org.tensortapestry.zspace.indexing.IndexingFns;

public class CellWiseAccumulatorMetaKernel extends DataTypeCheckingMetaKernel {

  public CellWiseAccumulatorMetaKernel(String kernelName, Set<String> dataTypes) {
    super(kernelName, dataTypes);
  }

  @Nonnull
  @Override
  public OperationNode apply(
    @Nonnull LoomGraph graph,
    @Nullable Map<String, List<TensorSelection>> inputs,
    @Nullable Map<String, Object> params
  ) {
    if (params != null && !params.isEmpty()) {
      throw new IllegalArgumentException("`%s` takes no parameters.".formatted(getKernelName()));
    }

    var expectedKeys = Set.of("tensors");
    if (!(inputs != null && inputs.keySet().equals(expectedKeys))) {
      throw new IllegalArgumentException(
        "Unexpected input keys, found %s, expected %s".formatted(inputs.keySet(), expectedKeys)
      );
    }

    var tensorSelections = inputs.get("tensors");

    var dtype = uniformDtypeCheck(graph, tensorSelections);

    var shapes = tensorSelections.stream().map(ts -> ts.getRange().getShape().toArray()).toList();
    var shape = IndexingFns.commonBroadcastShape(shapes.toArray(int[][]::new));

    // TODO: broadcasting requires changing the projection maps.

    var result = TensorNode
      .on(graph)
      .label("%s.result".formatted(getKernelName()))
      .body(b -> b.dtype(dtype).shape(shape))
      .build();

    var outputs = Map.of("result", List.of(TensorSelection.from(result)));

    var op = OperationNode
      .on(graph)
      .label(getKernelName())
      .body(b -> {
        b.kernel(getKernelName());
        b.inputs(inputs);
        b.outputs(outputs);
      })
      .build();

    var index = ZRange.newFromShape(shape);
    op.addAnnotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, index);

    op.addAnnotation(
      TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE,
      IPFSignature
        .builder()
        .input(
          "tensors",
          tensorSelections
            .stream()
            .map(ts ->
              ZRangeProjectionMap
                .builder()
                .affineMap(ZAffineMap.newBroadcastMatrix(shape.length, ts.getShape()))
                .translate(ts.getRange().getStart())
                .build()
            )
            .toList()
        )
        .output("result", ZRangeProjectionMap.builder().identityMap(shape.length).build())
        .build()
    );

    OperationUtils.createIpfShards(op, List.of(index));

    return op;
  }
}
