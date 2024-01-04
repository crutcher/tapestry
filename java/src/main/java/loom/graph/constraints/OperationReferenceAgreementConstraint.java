package loom.graph.constraints;

import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import loom.common.json.JsonPathUtils;
import loom.common.lazy.LazyString;
import loom.common.lazy.Thunk;
import loom.graph.*;
import loom.graph.nodes.*;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZRange;

public class OperationReferenceAgreementConstraint implements LoomEnvironment.Constraint {
  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertClassForType(TensorNode.TYPE, TensorNode.class);
    env.assertClassForType(OperationSignatureNode.TYPE, OperationSignatureNode.class);
    env.assertClassForType(ApplicationNode.TYPE, ApplicationNode.class);
  }

  @Override
  public void validateConstraint(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    for (var node :
        graph.nodeScan().type(ApplicationNode.TYPE).nodeClass(ApplicationNode.class).asList()) {
      var opNode =
          ValidationUtils.validateNodeReference(
              graph,
              node.getOperationId(),
              OperationSignatureNode.TYPE,
              OperationSignatureNode.class,
              new LazyString(
                  () -> JsonPathUtils.concatJsonPath(node.getJsonPath(), "body", "operationId")),
              issueCollector,
              Thunk.of(() -> List.of(node.asValidationContext("Application Node"))));

      if (opNode == null) {
        valid = false;
      }
    }

    for (var node :
        graph
            .nodeScan()
            .type(OperationSignatureNode.TYPE)
            .nodeClass(OperationSignatureNode.class)
            .asList()) {
      valid &= validateOperationSignatureNode(graph, node, issueCollector);
    }

    if (valid) {
      for (var cycle : TraversalUtils.findOperationSimpleCycles(graph)) {
        var cycleDesc =
            cycle.stream()
                .map(
                    item -> {
                      var desc = new HashMap<>();
                      desc.put("id", item.getId());
                      desc.put("type", item.getType());
                      if (item.getLabel() != null) {
                        desc.put("label", item.getLabel());
                      }
                      return desc;
                    })
                .toList();

        issueCollector.addIssue(
            ValidationIssue.builder()
                .type(LoomConstants.REFERENCE_CYCLE_ERROR)
                .summary("Reference Cycle detected")
                .context(b -> b.name("Cycle").data(cycleDesc)));
      }
    }
  }

  private static boolean validateOperationSignatureNode(
      LoomGraph graph,
      OperationSignatureNode opSignatureNode,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    var lazyContexts =
        Thunk.of(() -> List.of(opSignatureNode.asValidationContext("Operation Node")));

    valid &=
        validateTensorSliceMap(
            graph,
            opSignatureNode,
            "inputs",
            opSignatureNode.getInputs(),
            lazyContexts,
            issueCollector);
    valid &=
        validateTensorSliceMap(
            graph,
            opSignatureNode,
            "outputs",
            opSignatureNode.getOutputs(),
            lazyContexts,
            issueCollector);

    var shards = opSignatureNode.getApplicationNodes();
    var shardIds = shards.stream().map(LoomNode::getId).toList();

    if (shards.isEmpty()) {
      // There are no shards, so we can't validate the shard ranges.
      issueCollector.addIssue(
          ValidationIssue.builder()
              .type(LoomConstants.NODE_VALIDATION_ERROR)
              .summary("Operation Signature has no Application shards")
              .withContexts(lazyContexts));
      return false;
    }
    for (var appNode : shards) {
      valid &= validateApplicationNode(opSignatureNode, appNode, issueCollector);
    }

    if (!valid) {
      return false;
    }

    valid =
        validateShardAgreement(
            shardIds,
            shards,
            "inputs",
            opSignatureNode.getInputs(),
            ApplicationNode::getInputs,
            lazyContexts,
            issueCollector);
    valid &=
        validateShardAgreement(
            shardIds,
            shards,
            "outputs",
            opSignatureNode.getOutputs(),
            ApplicationNode::getOutputs,
            lazyContexts,
            issueCollector);

    // check the output range coverage:
    // 3. The sum of the shard range sizes is equal to the output range size.
    for (var entry : opSignatureNode.getOutputs().entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      final var k = selections.size();

      var shardSelections = shards.stream().map(shard -> shard.getOutputs().get(ioName)).toList();

      for (int idx = 0; idx < k; ++idx) {
        var sigRange = selections.get(idx).getRange();
        var finalIdx = idx;

        var shardRanges = shardSelections.stream().map(s -> s.get(finalIdx).getRange()).toList();

        var totalSize = shardRanges.stream().mapToInt(ZRange::getSize).sum();
        if (totalSize != sigRange.getSize()) {
          // There are overlapping shard ranges.
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary("Overlapping Application output key \"%s[%d]\" ranges", ioName, idx)
                  .context(
                      ValidationIssue.Context.builder()
                          .name("Application Shard Ranges")
                          .data(rangeMap(shardIds, shardRanges)))
                  .withContexts(lazyContexts));
          valid = false;
        }
      }
    }

    return valid;
  }

  private static boolean validateShardAgreement(
      List<UUID> shardIds,
      List<ApplicationNode> shards,
      String sliceMapName,
      Map<String, List<TensorSelection>> selectionMap,
      Function<ApplicationNode, Map<String, List<TensorSelection>>> shardSelectionMapFn,
      Supplier<List<ValidationIssue.Context>> contextsSupplier,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    for (var entry : selectionMap.entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      final var k = selections.size();

      var shardSelections =
          shards.stream().map(shard -> shardSelectionMapFn.apply(shard).get(ioName)).toList();
      assert shardSelections.stream().allMatch(s -> s.size() == k);

      for (int idx = 0; idx < k; ++idx) {
        var sigRange = selections.get(idx).getRange();
        var finalIdx = idx;
        var shardRanges = shardSelections.stream().map(s -> s.get(finalIdx).getRange()).toList();

        var boundingRange = ZRange.boundingRange(shardRanges);

        if (!sigRange.equals(boundingRange)) {

          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary(
                      "Operation Signature %s key \"%s[%d]\" range %s != shard bounding range %s",
                      sliceMapName, ioName, idx, sigRange, boundingRange)
                  .context(
                      ValidationIssue.Context.builder()
                          .name("Application Shard Ranges")
                          .data(rangeMap(shardIds, shardRanges))
                          .build())
                  .withContexts(contextsSupplier));
          valid = false;
        }
      }
    }
    return valid;
  }

  @SuppressWarnings("UnstableApiUsage")
  private static Map<UUID, ZRange> rangeMap(List<UUID> shardIds, List<ZRange> shardRanges) {
    return Streams.zip(shardIds.stream(), shardRanges.stream(), Map::entry)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static boolean validateApplicationNode(
      OperationSignatureNode operationSignatureNode,
      ApplicationNode applicationNode,
      ValidationIssueCollector issueCollector) {

    var lazyContexts =
        Thunk.of(
            () ->
                List.of(
                    applicationNode.asValidationContext("Application Node"),
                    operationSignatureNode.asValidationContext("Operation Node")));

    boolean valid = true;
    valid &=
        validateApplicationSignatureAgreement(
            applicationNode,
            operationSignatureNode,
            "inputs",
            applicationNode.getInputs(),
            operationSignatureNode.getInputs(),
            lazyContexts,
            issueCollector);
    valid &=
        validateApplicationSignatureAgreement(
            applicationNode,
            operationSignatureNode,
            "outputs",
            applicationNode.getOutputs(),
            operationSignatureNode.getOutputs(),
            lazyContexts,
            issueCollector);

    return valid;
  }

  public static boolean validateApplicationSignatureAgreement(
      ApplicationNode appNode,
      OperationSignatureNode opSig,
      String sliceMapName,
      Map<String, List<TensorSelection>> appSelectionMap,
      Map<String, List<TensorSelection>> sigSelectionMap,
      Supplier<List<ValidationIssue.Context>> contextsSupplier,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    var appKeys = appSelectionMap.keySet();
    var sigKeys = sigSelectionMap.keySet();
    if (!appKeys.equals(sigKeys)) {
      issueCollector.addIssue(
          ValidationIssue.builder()
              .type(LoomConstants.NODE_VALIDATION_ERROR)
              .summary(
                  "Application Node %s keys %s != Operation Signature %s keys %s",
                  sliceMapName,
                  appKeys.stream().sorted().toList(),
                  sliceMapName,
                  sigKeys.stream().sorted().toList())
              .withContexts(contextsSupplier));
      valid = false;
    } else {
      for (var entry : appSelectionMap.entrySet()) {
        final var ioName = entry.getKey();
        final var appSelections = entry.getValue();
        final var sigSelections = sigSelectionMap.get(ioName);

        if (appSelections.size() != sigSelections.size()) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .summary(
                      "Application %s key \"%s\" selection size (%d) != Signature selection size (%d)",
                      sliceMapName, ioName, appSelections.size(), sigSelections.size())
                  .withContexts(contextsSupplier));
          valid = false;
        } else {
          for (int idx = 0; idx < appSelections.size(); ++idx) {
            var appSelection = appSelections.get(idx);
            var sigSelection = sigSelections.get(idx);

            var finalIdx = idx;

            Supplier<List<ValidationIssue.Context>> localContext =
                () ->
                    List.of(
                        ValidationIssue.Context.builder()
                            .name("Application Tensor Selection")
                            .jsonpath(
                                JsonPathUtils.concatJsonPath(
                                    appNode.getJsonPath(),
                                    "body",
                                    sliceMapName,
                                    ioName,
                                    "[%d]".formatted(finalIdx)))
                            .data(appSelection)
                            .build(),
                        ValidationIssue.Context.builder()
                            .name("Operation Tensor Selection")
                            .jsonpath(
                                JsonPathUtils.concatJsonPath(
                                    opSig.getJsonPath(),
                                    "body",
                                    sliceMapName,
                                    ioName,
                                    "[%d]".formatted(finalIdx)))
                            .data(sigSelection)
                            .build());

            if (!appSelection.getTensorId().equals(sigSelection.getTensorId())) {
              issueCollector.addIssue(
                  ValidationIssue.builder()
                      .type(LoomConstants.NODE_VALIDATION_ERROR)
                      .summary("Application Tensor Selection Tensor Id != Signature Tensor Id")
                      .withContexts(localContext)
                      .withContexts(contextsSupplier));
              valid = false;
            } else {
              if (!sigSelection.getRange().contains(appSelection.getRange())) {
                issueCollector.addIssue(
                    ValidationIssue.builder()
                        .type(LoomConstants.NODE_VALIDATION_ERROR)
                        .summary(
                            "Application Tensor Selection range %s is outside signature range %s",
                            appSelection.getRange(), sigSelection.getRange())
                        .withContexts(localContext)
                        .withContexts(contextsSupplier));
                valid = false;
              }
            }
          }
        }
      }
    }
    return valid;
  }

  private static boolean validateTensorSliceMap(
      LoomGraph graph,
      LoomNode<?, ?> node,
      String sliceMapName,
      Map<String, List<TensorSelection>> selectionMap,
      Supplier<List<ValidationIssue.Context>> contextsSupplier,
      ValidationIssueCollector issueCollector) {
    boolean valid = true;
    for (var entry : selectionMap.entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      var fieldPath =
          LazyString.of(
              () -> JsonPathUtils.concatJsonPath(node.getJsonPath(), "body", sliceMapName, ioName));

      for (int idx = 0; idx < selections.size(); ++idx) {
        var tensorSelection = selections.get(idx);

        var itemPath = LazyString.format("%s[%d]", fieldPath, idx);

        TensorNode tensorNode =
            ValidationUtils.validateNodeReference(
                graph,
                tensorSelection.getTensorId(),
                TensorNode.TYPE,
                TensorNode.class,
                itemPath,
                issueCollector,
                contextsSupplier);

        if (tensorNode == null) {
          valid = false;
          continue;
        }
        var tensorRange = tensorNode.getEffectiveRange();

        var selectionRange = tensorSelection.getRange();
        var lazySelectionRangeContext =
            Thunk.of(
                () ->
                    ValidationIssue.Context.builder()
                        .name("Selection Range")
                        .jsonpath(itemPath)
                        .data(selectionRange)
                        .build());

        if (selectionRange.getNDim() != tensorRange.getNDim()) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .param("nodeType", OperationSignatureNode.TYPE)
                  .param("expectedDimensions", selectionRange.getNDim())
                  .param("actualDimensions", tensorRange.getNDim())
                  .summary("Tensor selection has the wrong number of dimensions")
                  .context(lazySelectionRangeContext)
                  .context(tensorNode.asValidationContext("Tensor Node"))
                  .withContexts(contextsSupplier)
                  .build());
          valid = false;
          continue;
        }

        if (!tensorRange.contains(selectionRange)) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .param("nodeType", OperationSignatureNode.TYPE)
                  .summary("Tensor selection is out of bounds")
                  .context(lazySelectionRangeContext)
                  .context(tensorNode.asValidationContext("Tensor Node"))
                  .withContexts(contextsSupplier)
                  .build());
          valid = false;
        }
      }
    }
    return valid;
  }
}
