package org.tensortapestry.loom.graph.dialects.tensorops;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.tensortapestry.common.json.JsonPathUtils;
import org.tensortapestry.common.lazy.LazyString;
import org.tensortapestry.common.lazy.Thunk;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.*;
import org.tensortapestry.zspace.ZRange;

public class OperationApplicationAgreementConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertSupportsNodeType(TensorNode.TYPE);
    env.assertSupportsNodeType(OperationNode.TYPE);
    env.assertSupportsNodeType(ApplicationNode.TYPE);
    env.assertConstraint(TensorOperationAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
    @SuppressWarnings("unused") LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    for (var application : graph.byType(ApplicationNode.class)) {
      ValidationUtils.validateNodeReference(
        graph,
        application.getOperationId(),
        OperationNode.TYPE,
        new LazyString(() ->
          JsonPathUtils.concatJsonPath(application.getJsonPath(), "body", "operationId")
        ),
        issueCollector,
        Thunk.of(() -> List.of(application.asValidationContext("Application Node")))
      );
    }

    for (var operation : graph.byType(OperationNode.class)) {
      validateOperationNode(operation, issueCollector);
    }
  }

  private static void validateOperationNode(
    OperationNode operation,
    ValidationIssueCollector issueCollector
  ) {
    var lazyContexts = Thunk.of(() -> List.of(operation.asValidationContext("Operation Node")));

    var shards = operation.getApplicationNodes().toList();
    var shardIds = shards.stream().map(ApplicationNode::getId).toList();

    if (shards.isEmpty()) {
      // There are no shards, so we can't validate the shard ranges.
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
          .summary("Operation Signature has no Application shards")
          .withContexts(lazyContexts)
      );
      return;
    }
    boolean valid = true;
    for (var appNode : shards) {
      valid &= validateApplicationNode(operation, appNode, issueCollector);
    }

    if (!valid) {
      return;
    }

    validateShardAgreement(
      shardIds,
      shards,
      "inputs",
      operation.getInputs(),
      ApplicationNode::getInputs,
      lazyContexts,
      issueCollector
    );
    validateShardAgreement(
      shardIds,
      shards,
      "outputs",
      operation.getOutputs(),
      ApplicationNode::getOutputs,
      lazyContexts,
      issueCollector
    );
  }

  private static boolean validateShardAgreement(
    List<UUID> shardIds,
    List<ApplicationNode> shards,
    String sliceMapName,
    Map<String, List<TensorSelection>> selectionMap,
    Function<ApplicationNode, Map<String, List<TensorSelection>>> shardSelectionMapFn,
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
    OperationNode operation,
    ApplicationNode application,
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
      validateTensorSelectionMapAgreement(
        application,
        operation,
        "inputs",
        application.getInputs(),
        operation.getInputs(),
        lazyContexts,
        issueCollector
      );
    valid &=
      validateTensorSelectionMapAgreement(
        application,
        operation,
        "outputs",
        application.getOutputs(),
        operation.getOutputs(),
        lazyContexts,
        issueCollector
      );

    return valid;
  }

  public static boolean validateTensorSelectionMapAgreement(
    ApplicationNode application,
    OperationNode operation,
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
}
