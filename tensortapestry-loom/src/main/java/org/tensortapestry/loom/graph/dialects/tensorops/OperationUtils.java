package org.tensortapestry.loom.graph.dialects.tensorops;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;

@UtilityClass
public class OperationUtils {

  public OperationNode applyRelativeSignature(
    LoomGraph graph,
    String kernelName,
    IPFSignature ipfSignature,
    Function<Map<String, List<TensorSelection>>, ZRange> indexBuilder,
    Function<ZRange, List<ZRange>> shardBuilder,
    Map<String, List<TensorSelection>> inputs,
    Map<String, List<String>> outputTypes,
    @Nullable Map<String, Object> params
  ) {
    var relativeSignature = IPFSignature.builder();
    for (var entry : ipfSignature.getInputs().entrySet()) {
      var name = entry.getKey();
      var projections = entry.getValue();
      var selections = inputs.get(name);
      assert selections != null && projections.size() == selections.size();

      List<ZRangeProjectionMap> relativeProjections = new ArrayList<>();
      for (int idx = 0; idx < selections.size(); ++idx) {
        var p = projections.get(idx);
        var s = selections.get(idx);
        relativeProjections.add(p.translate(s.getRange().getStart()));
      }
      relativeSignature.input(name, relativeProjections);
    }

    relativeSignature.outputs(ipfSignature.getOutputs());

    return applyFixedSignature(
      graph,
      kernelName,
      relativeSignature.build(),
      indexBuilder,
      shardBuilder,
      inputs,
      outputTypes,
      params
    );
  }

  public OperationNode applyFixedSignature(
    LoomGraph graph,
    String kernelName,
    IPFSignature ipfSignature,
    Function<Map<String, List<TensorSelection>>, ZRange> indexBuilder,
    Function<ZRange, List<ZRange>> shardBuilder,
    Map<String, List<TensorSelection>> inputs,
    Map<String, List<String>> outputTypes,
    @Nullable Map<String, Object> params
  ) {
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

    var ipfIndex = indexBuilder.apply(inputs);

    var operation = OperationNode
      .on(graph)
      .annotation(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE, ipfSignature)
      .annotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, ipfIndex)
      .body(b -> {
        b.kernel(kernelName);

        if (params != null) {
          b.params(params);
        }

        for (var entry : inputs.entrySet()) {
          var name = entry.getKey();
          var selections = entry.getValue();
          var projections = ipfSignature.getInputs().get(name);
          assert projections != null && projections.size() == selections.size();

          for (int idx = 0; idx < selections.size(); ++idx) {
            var s = selections.get(idx);
            var p = projections.get(idx);
            assert s.getRange().equals(p.apply(ipfIndex));
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

            var tensor = TensorNode
              .builder()
              .label("%s/%s[%d]".formatted(kernelName, name, idx))
              .graph(graph)
              .body(tb -> tb.dtype(t).range(p.apply(ipfIndex)))
              .build();

            selections.add(TensorSelection.from(tensor));
          }
          b.output(name, selections);
        }
      })
      .build();

    createIpfShards(operation, shardBuilder.apply(ipfIndex));

    return operation;
  }

  @CanIgnoreReturnValue
  public List<ApplicationNode> createIpfShards(
    OperationNode operation,
    Collection<ZRange> shardIndexes
  ) {
    return shardIndexes.stream().map(shardIndex -> createIpfShard(operation, shardIndex)).toList();
  }

  @CanIgnoreReturnValue
  public ApplicationNode createIpfShard(OperationNode operation, ZRange shardIndex) {
    var ipfSig = operation.viewTagAs(
      TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE,
      IPFSignature.class
    );
    var ipfIndex = operation.viewTagAs(
      TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE,
      ZRange.class
    );

    assert ipfIndex.contains(shardIndex);

    return ApplicationNode
      .on(operation.assertGraph())
      .annotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, shardIndex)
      .body(b -> {
        b.operationId(operation.getId());

        for (var entry : ipfSig.getInputs().entrySet()) {
          var name = entry.getKey();
          var projections = entry.getValue();
          var baseSelections = operation.getBody().getInputs().get(name);
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
          var baseSelections = operation.getBody().getOutputs().get(name);
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
      .build();
  }
}
