package org.tensortapestry.weft.metakernels;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import guru.nidi.graphviz.engine.Format;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.export.graphviz.GraphVisualizer;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;
import org.tensortapestry.zspace.indexing.IndexingFns;

public class MKTest implements CommonAssertions {

  public abstract static class MetaKernel {

    @Data
    public class CallBuilder {

      private LoomGraph graph;

      @Nonnull
      private Map<String, List<TensorSelection>> inputs = new HashMap<>();

      @Nonnull
      private Map<String, Object> params = new HashMap<>();

      @CanIgnoreReturnValue
      public CallBuilder graph(LoomGraph graph) {
        this.graph = graph;
        return this;
      }

      @CanIgnoreReturnValue
      public CallBuilder input(String key, List<TensorSelection> items) {
        this.inputs.put(key, items);
        return this;
      }

      @CanIgnoreReturnValue
      public CallBuilder param(String key, Object value) {
        this.params.put(key, value);
        return this;
      }

      public OperationNode apply() {
        return MetaKernel.this.apply(graph, inputs, params);
      }
    }

    public CallBuilder on(@Nonnull LoomGraph graph) {
      return new CallBuilder().graph(graph);
    }

    public abstract OperationNode apply(
      @Nonnull LoomGraph graph,
      @Nullable Map<String, List<TensorSelection>> inputs,
      @Nullable Map<String, Object> params
    );
  }

  public static class AddKernel extends MetaKernel {

    private final String kernelName = "ops:add";

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
        .builder()
        .graph(graph)
        .label("%s.result".formatted(kernelName))
        .configure(c -> c.dtype("int32").shape(shape))
        .build();

      var outputs = Map.of("result", List.of(TensorSelection.from(result)));

      var op = OperationNode
        .builder()
        .graph(graph)
        .label(kernelName)
        .configure(c -> {
          c.kernel(kernelName);
          c.inputs(inputs);
          c.outputs(outputs);
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

  @Test
  @SuppressWarnings("unused")
  public void test() {
    var env = CommonEnvironments.expressionEnvironment();
    var graph = env.newGraph();

    var tensorA = TensorNode
      .builder(graph)
      .label("A")
      .configure(c -> c.dtype("int32").shape(10, 10))
      .build();

    var tensorB = TensorNode
      .builder(graph)
      .label("B")
      .configure(c -> c.dtype("int32").shape(10, 10))
      .build();

    var add = new AddKernel();

    var op = add
      .on(graph)
      .input("tensors", List.of(TensorSelection.from(tensorA), TensorSelection.from(tensorB)))
      .apply();

    graph.validate();
    var exporter = GraphVisualizer.buildDefault();
    var export = exporter.export(graph);
    var gv = export.getGraphviz();
    var img = gv.render(Format.PNG).toImage();
  }
}
