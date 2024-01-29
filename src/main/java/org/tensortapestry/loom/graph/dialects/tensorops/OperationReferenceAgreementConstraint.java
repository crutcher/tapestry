package org.tensortapestry.loom.graph.dialects.tensorops;

import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.tensortapestry.loom.common.json.JsonPathUtils;
import org.tensortapestry.loom.common.lazy.LazyString;
import org.tensortapestry.loom.common.lazy.Thunk;
import org.tensortapestry.loom.graph.*;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.validation.ValidationIssueCollector;
import org.tensortapestry.loom.zspace.ZRange;

public class OperationReferenceAgreementConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertSupportsNodeType(TensorOpNodes.TENSOR_NODE_TYPE);
    env.assertSupportsNodeType(TensorOpNodes.OPERATION_NODE_TYPE);
    env.assertSupportsNodeType(TensorOpNodes.APPLICATION_NODE_TYPE);
  }

  @Override
  public void validateConstraint(
    @SuppressWarnings("unused") LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    boolean valid = true;
    for (var node : graph.byType(TensorOpNodes.APPLICATION_NODE_TYPE)) {
      var appData = node.viewBodyAs(ApplicationBody.class);

      var opNode = ValidationUtils.validateNodeReference(
        graph,
        appData.getOperationId(),
        TensorOpNodes.OPERATION_NODE_TYPE,
        new LazyString(() -> JsonPathUtils.concatJsonPath(node.getJsonPath(), "body", "operationId")
        ),
        issueCollector,
        Thunk.of(() -> List.of(node.asValidationContext("Application Node")))
      );

      if (opNode == null) {
        valid = false;
      }
    }

    for (var node : graph.byType(TensorOpNodes.OPERATION_NODE_TYPE)) {
      valid &= validateOperationSignatureNode(graph, node, issueCollector);
    }

    if (valid) {
      for (var cycle : TraversalUtils.findOperationSimpleCycles(graph)) {
        var cycleDesc = cycle
          .stream()
          .map(item -> {
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
          ValidationIssue
            .builder()
            .type(LoomConstants.Errors.REFERENCE_CYCLE_ERROR)
            .summary("Reference Cycle detected")
            .context(b -> b.name("Cycle").data(cycleDesc))
        );
      }
    }
  }

  private static boolean validateOperationSignatureNode(
    LoomGraph graph,
    LoomNode operation,
    ValidationIssueCollector issueCollector
  ) {
    operation.assertType(TensorOpNodes.OPERATION_NODE_TYPE);
    var opData = operation.viewBodyAs(OperationBody.class);

    boolean valid = true;
    var lazyContexts = Thunk.of(() -> List.of(operation.asValidationContext("Operation Node")));

    valid &=
      validateTensorSliceMap(
        graph,
        operation,
        "inputs",
        opData.getInputs(),
        lazyContexts,
        issueCollector
      );
    valid &=
      validateTensorSliceMap(
        graph,
        operation,
        "outputs",
        opData.getOutputs(),
        lazyContexts,
        issueCollector
      );

    var shards = OperationBody.getShards(operation);
    var shardIds = shards.stream().map(LoomNode::getId).toList();

    if (shards.isEmpty()) {
      // There are no shards, so we can't validate the shard ranges.
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
          .summary("Operation Signature has no Application shards")
          .withContexts(lazyContexts)
      );
      return false;
    }
    for (var appNode : shards) {
      valid &= validateApplicationNode(operation, appNode, issueCollector);
    }

    if (!valid) {
      return false;
    }

    valid =
      validateShardAgreement(
        shardIds,
        shards,
        "inputs",
        opData.getInputs(),
        s -> s.viewBodyAs(ApplicationBody.class).getInputs(),
        lazyContexts,
        issueCollector
      );
    valid &=
      validateShardAgreement(
        shardIds,
        shards,
        "outputs",
        opData.getOutputs(),
        s -> s.viewBodyAs(ApplicationBody.class).getOutputs(),
        lazyContexts,
        issueCollector
      );

    // check the output range coverage:
    // 3. The sum of the shard range sizes is equal to the output range size.
    for (var entry : opData.getOutputs().entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      final var k = selections.size();

      var shardSelections = shards
        .stream()
        .map(s -> s.viewBodyAs(ApplicationBody.class).getOutputs().get(ioName))
        .toList();

      for (int idx = 0; idx < k; ++idx) {
        var sigRange = selections.get(idx).getRange();
        var finalIdx = idx;

        var shardRanges = shardSelections.stream().map(s -> s.get(finalIdx).getRange()).toList();

        var totalSize = shardRanges.stream().mapToInt(ZRange::getSize).sum();
        if (totalSize != sigRange.getSize()) {
          // There are overlapping shard ranges.
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
              .summary("Overlapping Application output key \"%s[%d]\" ranges", ioName, idx)
              .context(
                ValidationIssue.Context
                  .builder()
                  .name("Application Shard Ranges")
                  .data(rangeMap(shardIds, shardRanges))
              )
              .withContexts(lazyContexts)
          );
          valid = false;
        }
      }
    }

    return valid;
  }

  private static boolean validateShardAgreement(
    List<UUID> shardIds,
    List<LoomNode> shards,
    String sliceMapName,
    Map<String, List<TensorSelection>> selectionMap,
    Function<LoomNode, Map<String, List<TensorSelection>>> shardSelectionMapFn,
    Supplier<List<ValidationIssue.Context>> contextsSupplier,
    ValidationIssueCollector issueCollector
  ) {
    boolean valid = true;
    for (var entry : selectionMap.entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      final var k = selections.size();

      var shardSelections = shards
        .stream()
        .map(shard -> shardSelectionMapFn.apply(shard).get(ioName))
        .toList();
      assert shardSelections.stream().allMatch(s -> s.size() == k);

      for (int idx = 0; idx < k; ++idx) {
        var sigRange = selections.get(idx).getRange();
        var finalIdx = idx;
        var shardRanges = shardSelections.stream().map(s -> s.get(finalIdx).getRange()).toList();

        var boundingRange = ZRange.boundingRange(shardRanges);

        if (!sigRange.equals(boundingRange)) {
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
              .summary(
                "Operation Signature %s key \"%s[%d]\" range %s != shard bounding range %s",
                sliceMapName,
                ioName,
                idx,
                sigRange,
                boundingRange
              )
              .context(
                ValidationIssue.Context
                  .builder()
                  .name("Application Shard Ranges")
                  .data(rangeMap(shardIds, shardRanges))
                  .build()
              )
              .withContexts(contextsSupplier)
          );
          valid = false;
        }
      }
    }
    return valid;
  }

  @SuppressWarnings("UnstableApiUsage")
  private static Map<UUID, ZRange> rangeMap(List<UUID> shardIds, List<ZRange> shardRanges) {
    return Streams
      .zip(shardIds.stream(), shardRanges.stream(), Map::entry)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static boolean validateApplicationNode(
    LoomNode operation,
    LoomNode application,
    ValidationIssueCollector issueCollector
  ) {
    var lazyContexts = Thunk.of(() ->
      List.of(
        application.asValidationContext("Application Node"),
        operation.asValidationContext("Operation Node")
      )
    );

    boolean valid = true;
    valid &=
      validateApplicationSignatureAgreement(
        application,
        operation,
        "inputs",
        application.viewBodyAs(ApplicationBody.class).getInputs(),
        operation.viewBodyAs(OperationBody.class).getInputs(),
        lazyContexts,
        issueCollector
      );
    valid &=
      validateApplicationSignatureAgreement(
        application,
        operation,
        "outputs",
        application.viewBodyAs(ApplicationBody.class).getOutputs(),
        operation.viewBodyAs(OperationBody.class).getOutputs(),
        lazyContexts,
        issueCollector
      );

    return valid;
  }

  public static boolean validateApplicationSignatureAgreement(
    LoomNode application,
    LoomNode operation,
    String sliceMapName,
    Map<String, List<TensorSelection>> appSelectionMap,
    Map<String, List<TensorSelection>> sigSelectionMap,
    Supplier<List<ValidationIssue.Context>> contextsSupplier,
    ValidationIssueCollector issueCollector
  ) {
    boolean valid = true;
    var appKeys = appSelectionMap.keySet();
    var sigKeys = sigSelectionMap.keySet();
    if (!appKeys.equals(sigKeys)) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
          .summary(
            "Application Node %s keys %s != Operation Signature %s keys %s",
            sliceMapName,
            appKeys.stream().sorted().toList(),
            sliceMapName,
            sigKeys.stream().sorted().toList()
          )
          .withContexts(contextsSupplier)
      );
      valid = false;
    } else {
      for (var entry : appSelectionMap.entrySet()) {
        final var ioName = entry.getKey();
        final var appSelections = entry.getValue();
        final var sigSelections = sigSelectionMap.get(ioName);

        if (appSelections.size() != sigSelections.size()) {
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
              .summary(
                "Application %s key \"%s\" selection size (%d) != Signature selection size (%d)",
                sliceMapName,
                ioName,
                appSelections.size(),
                sigSelections.size()
              )
              .withContexts(contextsSupplier)
          );
          valid = false;
        } else {
          for (int idx = 0; idx < appSelections.size(); ++idx) {
            var appSelection = appSelections.get(idx);
            var sigSelection = sigSelections.get(idx);

            var finalIdx = idx;

            Supplier<List<ValidationIssue.Context>> localContext = () ->
              List.of(
                ValidationIssue.Context
                  .builder()
                  .name("Application Tensor Selection")
                  .jsonpath(
                    JsonPathUtils.concatJsonPath(
                      application.getJsonPath(),
                      "body",
                      sliceMapName,
                      ioName,
                      "[%d]".formatted(finalIdx)
                    )
                  )
                  .data(appSelection)
                  .build(),
                ValidationIssue.Context
                  .builder()
                  .name("Operation Tensor Selection")
                  .jsonpath(
                    JsonPathUtils.concatJsonPath(
                      operation.getJsonPath(),
                      "body",
                      sliceMapName,
                      ioName,
                      "[%d]".formatted(finalIdx)
                    )
                  )
                  .data(sigSelection)
                  .build()
              );

            if (!appSelection.getTensorId().equals(sigSelection.getTensorId())) {
              issueCollector.addIssue(
                ValidationIssue
                  .builder()
                  .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
                  .summary("Application Tensor Selection Tensor Id != Signature Tensor Id")
                  .withContexts(localContext)
                  .withContexts(contextsSupplier)
              );
              valid = false;
            } else {
              if (!sigSelection.getRange().contains(appSelection.getRange())) {
                issueCollector.addIssue(
                  ValidationIssue
                    .builder()
                    .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
                    .summary(
                      "Application Tensor Selection range %s is outside signature range %s",
                      appSelection.getRange(),
                      sigSelection.getRange()
                    )
                    .withContexts(localContext)
                    .withContexts(contextsSupplier)
                );
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
    LoomNode node,
    String sliceMapName,
    Map<String, List<TensorSelection>> selectionMap,
    Supplier<List<ValidationIssue.Context>> contextsSupplier,
    ValidationIssueCollector issueCollector
  ) {
    boolean valid = true;
    for (var entry : selectionMap.entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      var fieldPath = LazyString.of(() ->
        JsonPathUtils.concatJsonPath(node.getJsonPath(), "body", sliceMapName, ioName)
      );

      for (int idx = 0; idx < selections.size(); ++idx) {
        var tensorSelection = selections.get(idx);

        var itemPath = LazyString.format("%s[%d]", fieldPath, idx);

        LoomNode tensorNode = ValidationUtils.validateNodeReference(
          graph,
          tensorSelection.getTensorId(),
          TensorOpNodes.TENSOR_NODE_TYPE,
          itemPath,
          issueCollector,
          contextsSupplier
        );
        if (tensorNode == null) {
          valid = false;
          continue;
        }
        var tensorData = tensorNode.viewBodyAs(TensorBody.class);
        var tensorRange = tensorData.getRange();

        var selectionRange = tensorSelection.getRange();
        var lazySelectionRangeContext = Thunk.of(() ->
          ValidationIssue.Context
            .builder()
            .name("Selection Range")
            .jsonpath(itemPath)
            .data(selectionRange)
            .build()
        );

        if (selectionRange.getNDim() != tensorRange.getNDim()) {
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
              .param("nodeType", TensorOpNodes.OPERATION_NODE_TYPE)
              .param("expectedDimensions", selectionRange.getNDim())
              .param("actualDimensions", tensorRange.getNDim())
              .summary("Tensor selection has the wrong number of dimensions")
              .context(lazySelectionRangeContext)
              .context(tensorNode.asValidationContext("Tensor Node"))
              .withContexts(contextsSupplier)
              .build()
          );
          valid = false;
          continue;
        }

        if (!tensorRange.contains(selectionRange)) {
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
              .param("nodeType", TensorOpNodes.OPERATION_NODE_TYPE)
              .summary("Tensor selection is out of bounds")
              .context(lazySelectionRangeContext)
              .context(tensorNode.asValidationContext("Tensor Node"))
              .withContexts(contextsSupplier)
              .build()
          );
          valid = false;
        }
      }
    }
    return valid;
  }
}
