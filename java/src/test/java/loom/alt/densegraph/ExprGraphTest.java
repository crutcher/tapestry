package loom.alt.densegraph;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import loom.alt.linkgraph.expressions.DimensionMap;
import loom.alt.linkgraph.expressions.IndexProjectionFunction;
import loom.testing.CommonAssertions;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZTensor;
import org.apache.commons.math3.util.Pair;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExprGraphTest implements CommonAssertions {
  public static EGTensor loadTensor(ExprGraph graph, String ref, ZPoint shape, String dtype) {
    var result = graph.addNode(EGTensor.builder().shape(shape).dtype(dtype));

    var op = new ScopedName("tapestry.io", "load");
    var sig = graph.addCommonOpSignature(EGOpSignature.builder().op(op).external(true));

    graph.addNode(
        EGOperation.builder()
            .signature(sig.getId())
            .options(Map.of("source", ref))
            .results(Map.of("result", List.of(result.getId()))));

    return result;
  }

  public static ZPoint _concatShapes(int dim, List<ZPoint> shapes) {
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

  public static EGTensor concat(ExprGraph graph, int dim, List<EGTensor> tensors) {
    var dtypes = tensors.stream().map(EGTensor::getDtype).collect(Collectors.toSet());
    if (dtypes.size() != 1) {
      throw new IllegalArgumentException("Incompatible dtypes: " + dtypes);
    }
    var dtype = dtypes.iterator().next();

    var result =
        graph.addNode(
            EGTensor.builder()
                .shape(
                    _concatShapes(
                        dim, tensors.stream().map(EGTensor::getShape).collect(Collectors.toList())))
                .dtype(dtype));

    graph.addNode(
        EGOperation.builder()
            .signature(
                graph
                    .addNode(
                        EGOpSignature.builder()
                            .op(new ScopedName("tapestry.io", "concat"))
                            .external(false)
                            .build())
                    .getId())
            .option("dim", Integer.toString(dim))
            .input("input", tensors.stream().map(EGTensor::getId).collect(Collectors.toList()))
            .result("result", List.of(result.getId())));

    return result;
  }

  public static EGTensor loadTensorFromShards(
      ExprGraph graph, int dim, String dtype, List<Pair<String, ZPoint>> shards) {
    var tensors =
        shards.stream()
            .map(p -> loadTensor(graph, p.getFirst(), p.getSecond(), dtype))
            .collect(Collectors.toList());

    return concat(graph, dim, tensors);
  }

  @Test
  public void testExampleGraph() {
    final String float32 = "float32";
    final String float8 = "float8";

    var graph = new ExprGraph();
    assertThat(graph).isNotNull();

    var a =
        loadTensorFromShards(
            graph,
            0,
            float32,
            List.of(
                Pair.create("#ref0", new ZPoint(50, 20)),
                Pair.create("#ref1", new ZPoint(50, 20))));

    var b0 = graph.addNode(EGTensor.builder().shape(new ZPoint(100, 10)).dtype(float32));
    var b1 = graph.addNode(EGTensor.builder().shape(new ZPoint(100, 10)).dtype(float32));

    graph.addNode(
        EGOperation.builder()
            .signature(
                graph
                    .addNode(
                        EGOpSignature.builder()
                            .op(new ScopedName("tapestry.io", "split"))
                            .external(false)
                            .build())
                    .getId())
            .option("dim", "1")
            .option("size", "10")
            .input("input", List.of(a.getId()))
            .result("result", List.of(b0.getId(), b1.getId())));

    var c = graph.addNode(EGTensor.builder().shape(new ZPoint(100, 20)).dtype(float8));

    graph.addNode(
        EGOperation.builder()
            .signature(
                graph
                    .addNode(
                        EGOpSignature.builder()
                            .op(new ScopedName("tapestry.io", "cast"))
                            .external(false)
                            .build())
                    .getId())
            .option("dtype", float8)
            .input("input", List.of(b0.getId()))
            .result("result", List.of(c.getId())));

    var w = loadTensor(graph, "#ref2", new ZPoint(5, 10), float8);

    var y = graph.addNode(EGTensor.builder().shape(new ZPoint(100, 5)).dtype(float8));
    graph.addNode(
        EGOperation.builder()
            .signature(
                graph
                    .addNode(
                        EGOpSignature.builder()
                            .op(new ScopedName("tapestry.io", "dense"))
                            .external(false)
                            .attribute(
                                new ScopedName("tapestry.io", "polysig"),
                                EGPolyhedralSignature.builder()
                                    .indexMap(new DimensionMap("batch", "out"))
                                    .inputProjection(
                                        "input",
                                        List.of(
                                            IndexProjectionFunction.builder()
                                                .input(new DimensionMap("block", "out"))
                                                .output(new DimensionMap("block", "in"))
                                                .map(
                                                    ZAffineMap.builder()
                                                        .a(
                                                            ZTensor.matrix(
                                                                new int[][] {{1, 0}, {0, 0}}))
                                                        .b(ZTensor.vector(0, 0))
                                                        .build())
                                                .shape(new ZPoint(1, 20))
                                                .build()))
                                    .build()
                                    .toJsonString())
                            .build())
                    .getId())
            .input("input", List.of(c.getId()))
            .input("weight", List.of(w.getId()))
            .result("result", List.of(y.getId())));

    graph.addNode(
        EGOperation.builder()
            .withSignature(
                graph.addNode(
                    EGOpSignature.builder()
                        .op(new ScopedName("tapestry.io", "save"))
                        .external(true)
                        .build()))
            .option("target", "#ref3")
            .input("input", List.of(y.getId())));

    assertJsonEquals(
        graph,
        "{\"id\": \""
            + graph.id.toString()
            + "\", "
            + "\"nodes\":"
            + graph.getNodeMap().values().stream()
                .map(EGNodeBase::toJsonString)
                .collect(Collectors.joining(",", "[", "]"))
            + "}");

    var dot = EGExporter.builder().build().toGraph(graph);
    @SuppressWarnings("unused")
    var img = Graphviz.fromGraph(dot).render(Format.PNG).toImage();

    graph.validate();
  }
}
