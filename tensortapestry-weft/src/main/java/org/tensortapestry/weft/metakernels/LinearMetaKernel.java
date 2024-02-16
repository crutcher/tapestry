package org.tensortapestry.weft.metakernels;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.tensortapestry.common.collections.ListBuilder;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.weft.metakernels.expressions.DimShapeMatcher;
import org.tensortapestry.zspace.*;

public class LinearMetaKernel extends DataTypeCheckingMetaKernel {

  private static final String KERNEL_NAME = "op:linear";
  public static final DimShapeMatcher X_SHAPE_MATCHER = DimShapeMatcher.parse("[*batch, features]");
  public static final DimShapeMatcher A_SHAPE_MATCHER = DimShapeMatcher.parse("[features, out]");

  public static final DimShapeMatcher B_SHAPE_MATCHER = DimShapeMatcher.parse("[out]");

  public LinearMetaKernel(Set<String> dataTypes) {
    super(KERNEL_NAME, dataTypes);
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

    var expectedKeys = Set.of("x", "A", "b");
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

    var xSelection = requiredSingular("x", inputs);
    var xNode = graph.assertNode(xSelection.getTensorId(), TensorNode.class);
    var xLayout = X_SHAPE_MATCHER.match(xSelection.getShape());

    var dtype = checkDataType(xNode.getDtype());
    List<Integer> batch = xLayout.getShapes().getGroup("batch");
    var features = xLayout.getShapes().getDim("features");

    var aSelection = requiredSingular("A", inputs);
    var aNode = graph.assertNode(aSelection.getTensorId(), TensorNode.class);
    var aLayout = A_SHAPE_MATCHER.match(aSelection.getShape());

    var out = aLayout.getShapes().getDim("out");

    {
      var aFeatures = aLayout.getShapes().getDim("features");
      if (features != aFeatures) {
        throw new IllegalArgumentException(
          "Expected %s features, found %s".formatted(features, aFeatures)
        );
      }
    }
    if (!aNode.getDtype().equals(dtype)) {
      throw new IllegalArgumentException(
        "Expected dtype <%s>, found <%s>".formatted(dtype, aNode.getDtype())
      );
    }

    var index = ZRange.newFromShape(ListBuilder.<Integer>builder().addAll(batch).add(out).build());

    {
      // x projection
      var proj = ZTensor.newZeros(xSelection.getNDim(), index.getNDim());
      for (int i = 0; i < batch.size(); i++) {
        proj.set(new int[] { i, i }, 1);
      }

      var xShape = ZTensor.newOnes(xSelection.getNDim());
      xShape.set(new int[] { -1 }, features);

      ipfSigBuilder.input(
        "x",
        ZRangeProjectionMap
          .builder()
          .affineMap(proj)
          .translate(xSelection.getRange().getStart())
          .shape(xShape)
          .build()
      );
    }

    {
      // A projection
      var proj = ZTensor.newZeros(2, index.getNDim());
      proj.set(new int[] { 1, -1 }, 1);

      ipfSigBuilder.input(
        "A",
        ZRangeProjectionMap
          .builder()
          .affineMap(proj)
          .translate(aSelection.getRange().getStart())
          .shape(features, 1)
          .build()
      );
    }

    var bSelection = optionalSingular("b", inputs);
    if (bSelection != null) {
      var bNode = graph.assertNode(bSelection.getTensorId(), TensorNode.class);
      if (!bNode.getDtype().equals(dtype)) {
        throw new IllegalArgumentException(
          "Expected dtype %s, found %s".formatted(dtype, bNode.getDtype())
        );
      }

      var bLayout = B_SHAPE_MATCHER.match(bSelection.getShape());
      var bOut = bLayout.getShapes().getDim("out");
      if (out != bOut) {
        throw new IllegalArgumentException("Expected %s out, found %s".formatted(out, bOut));
      }

      {
        // b projection
        var proj = ZTensor.newZeros(1, index.getNDim());
        proj.set(new int[] { 0, -1 }, 1);

        ipfSigBuilder.input(
          "b",
          ZRangeProjectionMap
            .builder()
            .affineMap(proj)
            .translate(bSelection.getRange().getStart())
            .shape(1)
            .build()
        );
      }
    }

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
