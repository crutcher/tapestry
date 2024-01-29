package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.tensortapestry.loom.common.json.JsonPathUtils;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.validation.ValidationIssueCollector;
import org.tensortapestry.loom.zspace.ZRange;
import org.tensortapestry.loom.zspace.ZRangeProjectionMap;

public class IPFSignatureAgreementConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertSupportsNodeType(TensorOpNodes.TENSOR_NODE_TYPE);
    env.assertSupportsNodeType(TensorOpNodes.OPERATION_NODE_TYPE);
    env.assertSupportsNodeType(TensorOpNodes.APPLICATION_NODE_TYPE);
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
    @SuppressWarnings("unused") LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    for (var operation : graph.byType(TensorOpNodes.OPERATION_NODE_TYPE)) {
      if (operation.hasAnnotation(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE)) {
        checkOperation(operation, issueCollector);
      }
    }
  }

  private void checkOperation(LoomNode operation, ValidationIssueCollector issueCollector) {
    operation.assertType(TensorOpNodes.OPERATION_NODE_TYPE);
    var opData = operation.viewBodyAs(OperationBody.class);

    var ipfSignature = operation.viewAnnotationAs(
      TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE,
      IPFSignature.class
    );

    Supplier<List<ValidationIssue.Context>> lazyContexts = () ->
      List.of(
        ValidationIssue.Context
          .builder()
          .name("Operation Node")
          .jsonpath(JsonPathUtils.concatJsonPath(operation.getJsonPath(), "body"))
          .data(operation.getId())
          .build(),
        ValidationIssue.Context
          .builder()
          .name("Operation Signature")
          .jsonpath(operation.getJsonPath())
          .data(operation.getId())
          .build()
      );

    {
      if (!operation.hasAnnotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)) {
        issueCollector.addIssue(
          ValidationIssue
            .builder()
            .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
            .param("opSigId", operation.getId())
            .summary("Operation signature does not have an IPF index")
            .context(operation.asValidationContext("Operation Signature"))
            .withContexts(lazyContexts)
        );
        return;
      }

      var ipfIndex = operation.viewAnnotationAs(
        TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE,
        ZRange.class
      );

      validateProjectionAgreement(
        ipfIndex,
        "inputs",
        opData.getInputs(),
        ipfSignature.getInputs(),
        issueCollector,
        lazyContexts
      );
      validateProjectionAgreement(
        ipfIndex,
        "outputs",
        opData.getOutputs(),
        ipfSignature.getOutputs(),
        issueCollector,
        lazyContexts
      );
    }

    for (var appNode : OperationBody.getShards(operation)) {
      if (!appNode.hasAnnotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)) {
        issueCollector.addIssue(
          ValidationIssue
            .builder()
            .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
            .param("appNodeId", appNode.getId())
            .param("opSigId", operation.getId())
            .summary("Application node does not have an IPF index")
            .context(appNode.asValidationContext("Application Node"))
            .withContexts(lazyContexts)
        );
        continue;
      }
      var appData = appNode.viewBodyAs(ApplicationBody.class);
      var ipfIndex = appNode.viewAnnotationAs(
        TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE,
        ZRange.class
      );

      validateProjectionAgreement(
        ipfIndex,
        "inputs",
        appData.getInputs(),
        ipfSignature.getInputs(),
        issueCollector,
        lazyContexts
      );
      validateProjectionAgreement(
        ipfIndex,
        "outputs",
        appData.getOutputs(),
        ipfSignature.getOutputs(),
        issueCollector,
        lazyContexts
      );
    }
  }

  @SuppressWarnings("unused")
  private void validateProjectionAgreement(
    ZRange ipfIndex,
    String selectionMapName,
    Map<String, List<TensorSelection>> selectionMap,
    Map<String, List<ZRangeProjectionMap>> projectionMap,
    ValidationIssueCollector issueCollector,
    Supplier<List<ValidationIssue.Context>> lazyContexts
  ) {
    if (!selectionMap.keySet().equals(projectionMap.keySet())) {
      issueCollector.addIssue(
        ValidationIssue
          .builder()
          .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
          .param("selectionMapName", selectionMapName)
          .param("selectionMapKeys", selectionMap.keySet())
          .param("projectionMapKeys", projectionMap.keySet())
          .summary("Selection map and projection map have different keys")
          .withContexts(lazyContexts)
      );
      return;
    }

    for (var entry : selectionMap.entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();
      final var projections = projectionMap.get(ioName);

      if (selections.size() != projections.size()) {
        issueCollector.addIssue(
          ValidationIssue
            .builder()
            .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
            .param("selectionMapName", selectionMapName)
            .param("ioName", ioName)
            .param("selections", selections)
            .param("projections", projections)
            .summary("Selection map and projection map have different sizes")
            .withContexts(lazyContexts)
        );
        continue;
      }

      for (int idx = 0; idx < selections.size(); ++idx) {
        var selection = selections.get(idx);
        var projection = projections.get(idx);

        var expected = projection.apply(ipfIndex);

        ZRange projectedRange = selection.getRange();
        if (!projectedRange.equals(expected)) {
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
              .param("selectionMapName", selectionMapName)
              .param("ioName", ioName)
              .param("selection", selection)
              .param("projection", projection)
              .param("expected", expected)
              .summary("Selection map and projection map have different ranges")
              .withContexts(lazyContexts)
          );
        }
      }
    }
  }
}
