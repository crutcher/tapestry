package org.tensortapestry.weft.metakernels;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZAffineMap;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;
import org.tensortapestry.zspace.indexing.IndexingFns;

@RequiredArgsConstructor
public class CellWiseAccumulatorMetaKernel extends MetaKernel {

  private final String kernelName;

  private final Set<String> dataTypes;

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

    var tensorSelections = inputs.get("tensors");
    var tensors = tensorSelections
      .stream()
      .map(TensorSelection::getTensorId)
      .map(id -> graph.assertNode(id, TensorNode.class))
      .toList();
    var seenTypes = new HashSet<String>();
    for (var tensor : tensors) {
      String dtype = tensor.getDtype();
      seenTypes.add(dtype);
      if (!dataTypes.contains(dtype)) {
        throw new IllegalArgumentException(
          "Unexpected dtype %s, expected one of %s".formatted(dtype, dataTypes)
        );
      }
    }

    if (seenTypes.size() != 1) {
      throw new IllegalArgumentException(
        "Expected all tensors to have the same dtype, found %s".formatted(seenTypes)
      );
    }

    var dtype = seenTypes.iterator().next();

    var shapes = tensorSelections.stream().map(ts -> ts.getRange().getShape().toArray()).toList();
    var shape = IndexingFns.commonBroadcastShape(shapes.toArray(int[][]::new));

    // TODO: broadcasting requires changing the projection maps.

    var result = TensorNode
      .on(graph)
      .label("%s.result".formatted(kernelName))
      .body(b -> b.dtype(dtype).shape(shape))
      .build();

    var outputs = Map.of("result", List.of(TensorSelection.from(result)));

    var op = OperationNode
      .on(graph)
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
            .map(ts ->
              ZRangeProjectionMap
                .builder()
                .affineMap(ZAffineMap.newBroadcastMatrix(shape.length, ts.getShape()))
                .translate(ts.getRange().getStart())
                .build()
            )
            .toList()
        )
        .output("result", commonProjection)
        .build()
    );

    OperationUtils.createIpfShards(op, List.of(index));

    return op;
  }
}
