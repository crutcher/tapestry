package org.tensortapestry.loom.graph.dialects.tensorops.constraints;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.tensortapestry.common.json.JsonPathUtils;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.zspace.ZRange;
import org.tensortapestry.zspace.ZRangeProjectionMap;

/**
 * This constraint checks that each `Application` node with an `IPFIndex` tag selection maps
 * matching the projection of that index through the corresponding `Operation` node's `IPFSignature`
 * tag.
 */
public class ApplicationIPFSignatureAgreementConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(@Nonnull LoomEnvironment env) {
    env.assertSupportsNodeType(TensorNode.TYPE);
    env.assertSupportsNodeType(OperationNode.TYPE);
    env.assertSupportsNodeType(ApplicationNode.TYPE);
    env.assertConstraint(OperationIPFSignatureAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
    @Nonnull @SuppressWarnings("unused") LoomEnvironment env,
    @Nonnull LoomGraph graph,
    @Nonnull ValidationIssueCollector issueCollector
  ) {
    for (var operation : graph.byType(OperationNode.class)) {
      if (operation.hasTag(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE)) {
        checkOperation(operation, issueCollector);
      }
    }
  }

  private void checkOperation(OperationNode operation, ValidationIssueCollector issueCollector) {
    operation.assertType(OperationNode.TYPE);

    var ipfSignature = operation.viewTagAs(
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

    for (var appNode : operation.getApplicationNodes()) {
      if (!appNode.hasTag(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)) {
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
      var appData = appNode.viewBodyAs(ApplicationNode.Body.class);
      var ipfIndex = appNode.viewTagAs(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, ZRange.class);

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
