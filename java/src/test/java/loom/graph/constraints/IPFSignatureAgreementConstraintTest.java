package loom.graph.constraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import guru.nidi.graphviz.engine.Format;
import java.util.*;
import java.util.function.Function;
import javax.annotation.Nullable;
import loom.graph.CommonEnvironments;
import loom.graph.LoomGraph;
import loom.graph.nodes.*;
import loom.polyhedral.IndexProjectionFunction;
import loom.testing.BaseTestClass;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

@SuppressWarnings("unused")
public class IPFSignatureAgreementConstraintTest extends BaseTestClass {
  @Test
  public void test_valid_short() {
    var env = CommonEnvironments.expressionEnvironment();
    env.addConstraint(new IPFSignatureAgreementConstraint());

    var graph = env.newGraph();

    {
      var tensorA =
          TensorNode.withBody(
                  b ->
                      b.dtype("int32")
                          .range(
                              ZRange.builder()
                                  .start(ZPoint.of(-10, 4))
                                  .shape(ZPoint.of(3, 4))
                                  .build()))
              .label("A")
              .addTo(graph);

      var tensorB = TensorNode.withBody(b -> b.dtype("int32").shape(4, 5)).label("B").addTo(graph);

      var relativeSignature =
          IPFSignature.builder()
              .input(
                  "x",
                  IndexProjectionFunction.builder()
                      .affineMap(new int[][] {{1, 0}, {0, 0}})
                      .shape(ZPoint.of(1, tensorA.getShape().get(1)))
                      .build())
              .input(
                  "y",
                  IndexProjectionFunction.builder()
                      .affineMap(new int[][] {{0, 0}, {0, 1}})
                      .shape(ZPoint.of(tensorB.getShape().get(0), 1))
                      .build())
              .output(
                  "z",
                  IndexProjectionFunction.builder().affineMap(new int[][] {{1, 0}, {0, 1}}).build())
              .build();

      var op =
          applyRelativeSignature(
              graph,
              "matmul",
              relativeSignature,
              inputs -> {
                var x = inputs.get("x").getFirst().getRange().getShape().get(0);
                var y = inputs.get("y").getFirst().getRange().getShape().get(1);
                return ZRange.fromShape(x, y);
              },
              Map.of(
                  "x",
                  List.of(TensorSelection.from(tensorA)),
                  "y",
                  List.of(TensorSelection.from(tensorB))),
              Map.of("z", List.of("int32")),
              null);

      assertThat(graph.nodeScan().nodeClass(OperationSignatureNode.class).asStream().count())
          .isEqualTo(1);
      assertThat(graph.nodeScan().nodeClass(ApplicationNode.class).asStream().count()).isEqualTo(1);

      TensorNode tensorC =
          graph.assertNode(
              op.getOutputs().get("z").getFirst().getTensorId(), TensorNode.TYPE, TensorNode.class);

      assertThat(tensorC.getDtype()).isEqualTo("int32");
      assertThat(tensorC.getRange()).isEqualTo(ZRange.fromShape(3, 5));
      assertThat(tensorC.getLabel()).isEqualTo("matmul/z[0]");

      var shards = op.getApplicationNodes();
      assertThat(shards).hasSize(1);
      var app = shards.getFirst();
      assertThat(app.getOperationSignatureNode()).isSameAs(op);
      assertThat(app.assertAnnotation(IPFIndex.ANNOTATION_TYPE, IPFIndex.class).getRange())
          .isEqualTo(ZRange.fromShape(3, 5));
    }

    graph.validate();

    var exporter = GraphExporter.buildDefault();
    var export = exporter.export(graph);
    var gv = export.getGraphviz();

    var img = gv.render(Format.PNG).toImage();

    System.out.println("here");
  }

  public static OperationSignatureNode applyRelativeSignature(
      LoomGraph graph,
      String kernelName,
      IPFSignature ipfSignature,
      Function<Map<String, List<TensorSelection>>, ZRange> indexBuilder,
      Map<String, List<TensorSelection>> inputs,
      Map<String, List<String>> outputTypes,
      @Nullable Map<String, Object> params) {

    var relativeSignature = IPFSignature.builder();
    for (var entry : ipfSignature.getInputs().entrySet()) {
      var name = entry.getKey();
      var projections = entry.getValue();
      var selections = inputs.get(name);
      assert selections != null && projections.size() == selections.size();

      List<IndexProjectionFunction> relativeProjections = new ArrayList<>();
      for (int idx = 0; idx < selections.size(); ++idx) {
        var p = projections.get(idx);
        var s = selections.get(idx);
        relativeProjections.add(p.translate(s.getRange().getStart()));
      }
      relativeSignature.input(name, relativeProjections);
    }

    relativeSignature.outputs(ipfSignature.getOutputs());

    return applyFixedSignature(
        graph, kernelName, relativeSignature.build(), indexBuilder, inputs, outputTypes, params);
  }

  public static OperationSignatureNode applyFixedSignature(
      LoomGraph graph,
      String kernelName,
      IPFSignature ipfSignature,
      Function<Map<String, List<TensorSelection>>, ZRange> indexBuilder,
      Map<String, List<TensorSelection>> inputs,
      Map<String, List<String>> outputTypes,
      @Nullable Map<String, Object> params) {

    /*
     * NOTE: there is some weirdness here.
     *
     * If the input tensors have a range that does not start from (0, ...);
     * then we probably want to adjust the IPFSignature of the generated
     * OperationSignatureNode to reflect that.
     *
     * That's messy; and the alternative is to say that the TensorSelections
     * are always relative to the range of the input tensors; not absolute.
     * Which is a larger change.
     */

    var index = indexBuilder.apply(inputs);
    var ipfIndex = IPFIndex.builder().range(index).build();

    var opSigNode =
        OperationSignatureNode.withBody(
                b -> {
                  b.name(kernelName);

                  for (var entry : inputs.entrySet()) {
                    var name = entry.getKey();
                    var selections = entry.getValue();
                    var projections = ipfSignature.getInputs().get(name);
                    assert projections != null && projections.size() == selections.size();

                    for (int idx = 0; idx < selections.size(); ++idx) {
                      var s = selections.get(idx);
                      var p = projections.get(idx);
                      assert s.getRange().equals(p.apply(index));
                    }

                    b.input(name, selections);
                  }

                  for (var entry : outputTypes.entrySet()) {
                    var name = entry.getKey();
                    var types = entry.getValue();
                    var projections = ipfSignature.getOutputs().get(name);
                    assert projections != null && projections.size() == types.size();

                    List<TensorSelection> selections = new ArrayList<>();
                    for (int idx = 0; idx < projections.size(); ++idx) {
                      var p = projections.get(idx);
                      var t = types.get(idx);

                      var tensor =
                          TensorNode.withBody(tb -> tb.dtype(t).range(p.apply(index)))
                              .label("%s/%s[%d]".formatted(kernelName, name, idx))
                              .addTo(graph);

                      selections.add(TensorSelection.from(tensor));
                    }
                    b.output(name, selections);
                  }
                })
            .annotation(IPFSignature.ANNOTATION_TYPE, ipfSignature)
            .annotation(IPFIndex.ANNOTATION_TYPE, ipfIndex)
            .addTo(graph);

    createIpfShards(opSigNode, index);

    return opSigNode;
  }

  @CanIgnoreReturnValue
  public static List<ApplicationNode> createIpfShards(
      OperationSignatureNode sig, ZRange... shardIndexes) {
    return createIpfShards(sig, Arrays.asList(shardIndexes));
  }

  public static List<ApplicationNode> createIpfShards(
      OperationSignatureNode sig, Collection<ZRange> shardIndexes) {
    return shardIndexes.stream().map(shardIndex -> createIpfShard(sig, shardIndex)).toList();
  }

  public static ApplicationNode createIpfShard(OperationSignatureNode sig, ZRange shardIndex) {
    var graph = sig.assertGraph();

    var ipfSig = sig.assertAnnotation(IPFSignature.ANNOTATION_TYPE, IPFSignature.class);
    var ipfIndex = sig.assertAnnotation(IPFIndex.ANNOTATION_TYPE, IPFIndex.class);

    assert ipfIndex.getRange().contains(shardIndex);

    var label =
        (sig.getLabel() == null ? sig.getName() : sig.getLabel())
            + ": "
            + shardIndex.toRangeString();

    return ApplicationNode.withBody(
            b -> {
              b.operationId(sig.getId());

              for (var entry : ipfSig.getInputs().entrySet()) {
                var name = entry.getKey();
                var projections = entry.getValue();
                var baseSelections = sig.getInputs().get(name);
                assert baseSelections != null && projections.size() == baseSelections.size();

                List<TensorSelection> selections = new ArrayList<>();
                for (int idx = 0; idx < projections.size(); ++idx) {
                  var p = projections.get(idx);
                  var s = baseSelections.get(idx);
                  selections.add(new TensorSelection(s.getTensorId(), p.apply(shardIndex)));
                }
                b.input(name, selections);
              }

              for (var entry : ipfSig.getOutputs().entrySet()) {
                var name = entry.getKey();
                var projections = entry.getValue();
                var baseSelections = sig.getOutputs().get(name);
                assert baseSelections != null && projections.size() == baseSelections.size();

                List<TensorSelection> selections = new ArrayList<>();
                for (int idx = 0; idx < projections.size(); ++idx) {
                  var p = projections.get(idx);
                  var s = baseSelections.get(idx);
                  selections.add(new TensorSelection(s.getTensorId(), p.apply(shardIndex)));
                }
                b.output(name, selections);
              }
            })
        .annotation(IPFIndex.ANNOTATION_TYPE, IPFIndex.builder().range(shardIndex).build())
        .label(label)
        .addTo(graph);
  }
}
