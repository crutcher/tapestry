package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.tensortapestry.loom.common.json.JsonPathUtils;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.validation.ValidationIssue;
import org.tensortapestry.loom.validation.ValidationIssueCollector;
import org.tensortapestry.loom.zspace.ZRange;
import org.tensortapestry.loom.zspace.ZRangeProjectionMap;

public class IPFSignatureAgreementConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertClassForType(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class);
    env.assertClassForType(TensorOpNodes.OPERATION_NODE_TYPE, OperationNode.class);
    env.assertClassForType(TensorOpNodes.APPLICATION_NODE_TYPE, ApplicationNode.class);
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
    @SuppressWarnings("unused") LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    for (
      var it = graph
        .nodeScan()
        .type(TensorOpNodes.OPERATION_NODE_TYPE)
        .nodeClass(OperationNode.class)
        .asStream()
        .iterator();
      it.hasNext();
    ) {
      var opSig = it.next();
      if (opSig.hasAnnotation(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE)) {
        checkOperation(opSig, issueCollector);
      }
    }
  }

  private void checkOperation(OperationNode opSig, ValidationIssueCollector issueCollector) {
    var ipfSignature = opSig.assertAnnotation(
      TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE,
      IPFSignature.class
    );

    Supplier<List<ValidationIssue.Context>> lazyContexts = () ->
      List.of(
        ValidationIssue.Context
          .builder()
          .name("Operation Node")
          .jsonpath(JsonPathUtils.concatJsonPath(opSig.getJsonPath(), "body"))
          .data(opSig.getId())
          .build(),
        ValidationIssue.Context
          .builder()
          .name("Operation Signature")
          .jsonpath(opSig.getJsonPath())
          .data(opSig.getId())
          .build()
      );

    {
      var ipfIndex = opSig.getAnnotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, ZRange.class);
      if (ipfIndex == null) {
        issueCollector.addIssue(
          ValidationIssue
            .builder()
            .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
            .param("opSigId", opSig.getId())
            .summary("Operation signature does not have an IPF index")
            .context(opSig.asValidationContext("Operation Signature"))
            .withContexts(lazyContexts)
        );
        return;
      }

      validateProjectionAgreement(
        ipfIndex,
        "inputs",
        opSig.getInputs(),
        ipfSignature.getInputs(),
        issueCollector,
        lazyContexts
      );
      validateProjectionAgreement(
        ipfIndex,
        "outputs",
        opSig.getOutputs(),
        ipfSignature.getOutputs(),
        issueCollector,
        lazyContexts
      );
    }

    for (var appNode : opSig.getApplicationNodes()) {
      var ipfIndex = appNode.getAnnotation(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, ZRange.class);
      if (ipfIndex == null) {
        issueCollector.addIssue(
          ValidationIssue
            .builder()
            .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
            .param("appNodeId", appNode.getId())
            .param("opSigId", opSig.getId())
            .summary("Application node does not have an IPF index")
            .context(appNode.asValidationContext("Application Node"))
            .withContexts(lazyContexts)
        );
        continue;
      }

      validateProjectionAgreement(
        ipfIndex,
        "inputs",
        appNode.getInputs(),
        ipfSignature.getInputs(),
        issueCollector,
        lazyContexts
      );
      validateProjectionAgreement(
        ipfIndex,
        "outputs",
        appNode.getOutputs(),
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
