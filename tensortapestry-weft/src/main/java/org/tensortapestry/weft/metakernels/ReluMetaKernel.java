package org.tensortapestry.weft.metakernels;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;

public class ReluMetaKernel extends DataTypeCheckingMetaKernel {

  public ReluMetaKernel(Set<String> dataTypes) {
    super("op:relu", dataTypes);
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

    var expectedKeys = Set.of("tensor");
    if (inputs == null || !expectedKeys.containsAll(inputs.keySet())) {
      throw new IllegalArgumentException(
        "Unexpected input keys, found %s, expected %s".formatted(inputs.keySet(), expectedKeys)
      );
    }

    IPFSignature.IPFSignatureBuilder ipfSigBuilder = IPFSignature.builder();
    OperationNode.Body.BodyBuilder opBodyBuilder = OperationNode.Body
      .builder()
      .kernel(getKernelName())
      .inputs(inputs);

    var tensorSelection = requiredSingular("tensor", inputs);
    var tensor = graph.assertNode(tensorSelection.getTensorId(), TensorNode.class);
    var dtype = checkDataType(tensor.getDtype());

    var index = ZRange.newFromShape(tensorSelection.getShape());

    ipfSigBuilder.input(
      "tensor",
      ZRangeProjectionMap
        .builder()
        .identityMap(index.getNDim())
        .translate(tensorSelection.getStart())
        .build()
    );

    var result = TensorNode
      .on(graph)
      .label("%s.result".formatted(getKernelName()))
      .body(b -> b.dtype(dtype).shape(index.getShape()))
      .build();

    opBodyBuilder.output("result", List.of(TensorSelection.from(result)));
    ipfSigBuilder.output(
      "result",
      ZRangeProjectionMap.builder().identityMap(index.getNDim()).build()
    );

    var op = OperationNode
      .on(graph)
      .label(getKernelName())
      .body(opBodyBuilder.build())
      .annotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, index)
      .annotation(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE, ipfSigBuilder.build())
      .build();

    OperationUtils.createIpfShards(op, List.of(index));

    return op;
  }
}
