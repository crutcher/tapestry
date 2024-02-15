package org.tensortapestry.weft.metakernels;

import lombok.RequiredArgsConstructor;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;
import org.tensortapestry.zspace.indexing.IndexingFns;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class CellwiseGroupOp extends MetaKernel {

  private final String kernelName;

  @Override
  public OperationNode apply(
    @Nonnull LoomGraph graph,
    @Nullable Map<String, List<TensorSelection>> inputs,
    @Nullable Map<String, Object> params
  ) {
    if (params != null && !params.isEmpty()) {
      throw new IllegalArgumentException("`add` takes no parameters.");
    }

    var expectedKeys = Set.of("tensors");
    if (inputs == null || !inputs.keySet().equals(expectedKeys)) {
      throw new IllegalArgumentException(
        "Unexpected input keys, found %s, expected %s".formatted(inputs.keySet(), expectedKeys)
      );
    }

    var tensors = inputs.get("tensors");

    // TODO:
    // 1. Validate tensors exist.
    // 2. Validate shared dtype.

    var shapes = tensors.stream().map(ts -> ts.getRange().getShape().toArray()).toList();
    var shape = IndexingFns.commonBroadcastShape(shapes.toArray(int[][]::new));

    var result = TensorNode
      .builder(graph)
      .label("%s.result".formatted(kernelName))
      .body(b -> b.dtype("int32").shape(shape))
      .build();

    var outputs = Map.of("result", List.of(TensorSelection.from(result)));

    var op = OperationNode
      .builder(graph)
      .label(kernelName)
      .body(b -> {
        b.kernel(kernelName);
        b.inputs(inputs);
        b.outputs(outputs);
      })
      .build();

    var index = ZRange.newFromShape(shape);
    op.addAnnotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, index);

    var commonProjection = ZRangeProjectionMap.builder().identityMap(shape.length).build();

    op.addAnnotation(
      TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE,
      IPFSignature
        .builder()
        .input(
          "tensors",
          tensors
            .stream()
            .map(ts -> commonProjection.translate(ts.getRange().getStart()))
            .toList()
        )
        .output("result", commonProjection)
        .build()
    );

    OperationUtils.createIpfShards(op, List.of(index));

    return op;
  }
}
