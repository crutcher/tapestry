package loom.alt.linkgraph.graph;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import loom.zspace.ZPoint;
import org.apache.commons.math3.util.Pair;

public interface TGraphOperations {
  default TTensor loadTensor(TGraph graph, String ref, ZPoint shape, String dtype) {
    var op = graph.addNode(new TBlockOperator("load"));
    op.markAsIO();
    op.bindParameters(Map.of("source", ref));
    return op.bindResult("result", shape, dtype);
  }

  default ZPoint _concatShapes(int dim, List<ZPoint> shapes) {
    int[] concatShape = null;
    for (var shape : shapes) {
      if (concatShape == null) {
        concatShape = shape.toArray();
      } else {
        for (int i = 0; i < concatShape.length; i++) {
          if (i == dim) {
            concatShape[i] += shape.get(i);
          } else if (concatShape[i] != shape.get(i)) {
            throw new IllegalArgumentException(
                "Incompatible shapes: " + new ZPoint(concatShape) + " vs " + shape);
          }
        }
      }
    }
    if (concatShape == null) {
      throw new IllegalArgumentException("No shapes provided");
    }
    return new ZPoint(concatShape);
  }

  default TTensor concat(TGraph graph, int dim, List<TTensor> tensors) {
    var shape =
        _concatShapes(dim, tensors.stream().map(TTensor::getShape).collect(Collectors.toList()));
    var concatOp = graph.addNode(new TFusionOperator("concat"));

    var dtypes = tensors.stream().map(TTensor::getDtype).collect(Collectors.toSet());
    if (dtypes.size() != 1) {
      throw new IllegalArgumentException("Incompatible dtypes: " + dtypes);
    }
    var dtype = dtypes.iterator().next();

    for (int i = 0; i < tensors.size(); ++i) {
      var tensor = tensors.get(i);
      concatOp.bindInput("input/" + i, tensor);
    }

    return concatOp.bindResult("result", shape, dtype);
  }

  default TTensor loadTensorFromShards(
      TGraph graph, int dim, String dtype, List<Pair<String, ZPoint>> shards) {
    var tensors =
        shards.stream()
            .map(p -> loadTensor(graph, p.getFirst(), p.getSecond(), dtype))
            .collect(Collectors.toList());

    return concat(graph, dim, tensors);
  }
}
