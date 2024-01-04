package loom.graph.constraints;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import loom.common.json.JsonPathUtils;
import loom.common.lazy.LazyString;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.nodes.*;
import loom.polyhedral.IndexProjectionFunction;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZRange;

public class IPFSignatureAgreementConstraint implements LoomEnvironment.Constraint {
  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertClassForType(TensorNode.TYPE, TensorNode.class);
    env.assertClassForType(OperationSignatureNode.TYPE, OperationSignatureNode.class);
    env.assertClassForType(ApplicationNode.TYPE, ApplicationNode.class);
    env.assertClassForType(IPFSignatureNode.TYPE, IPFSignatureNode.class);
    env.assertClassForType(IPFIndexNode.TYPE, IPFIndexNode.class);
    env.assertConstraint(OperationReferenceAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {
    for (var it =
            graph
                .nodeScan()
                .type(OperationSignatureNode.TYPE)
                .nodeClass(OperationSignatureNode.class)
                .asStream()
                .iterator();
        it.hasNext(); ) {
      var opSig = it.next();

      var sigId = opSig.getSignatureId();
      var sigNode = graph.getNode(sigId);
      if (sigNode instanceof IPFSignatureNode ipfSignatureNode) {
        checkOperation(graph, ipfSignatureNode, opSig, issueCollector);
      }
    }
  }

  private void checkOperation(
      LoomGraph graph,
      IPFSignatureNode ipfSignatureNode,
      OperationSignatureNode opSig,
      ValidationIssueCollector issueCollector) {

    Supplier<List<ValidationIssue.Context>> lazyContexts =
        () ->
            List.of(
                ValidationIssue.Context.builder()
                    .name("Operation")
                    .jsonpath(JsonPathUtils.concatJsonPath(opSig.getJsonPath(), "body"))
                    .data(opSig.getId())
                    .build(),
                ValidationIssue.Context.builder()
                    .name("Operation Signature")
                    .jsonpath(opSig.getJsonPath())
                    .data(opSig.getId())
                    .build());

    {
      var indexNode =
          ValidationUtils.validateNodeReference(
              graph,
              opSig.getIndexId(),
              IPFIndexNode.TYPE,
              IPFIndexNode.class,
              new LazyString(
                  () -> JsonPathUtils.concatJsonPath(opSig.getJsonPath(), "body.indexId")),
              issueCollector,
              lazyContexts);
      if (indexNode == null) {
        return;
      }

      validateProjectionAgreement(
          indexNode,
          "inputs",
          opSig.getInputs(),
          ipfSignatureNode.getInputs(),
          issueCollector,
          lazyContexts);
      validateProjectionAgreement(
          indexNode,
          "outputs",
          opSig.getOutputs(),
          ipfSignatureNode.getOutputs(),
          issueCollector,
          lazyContexts);
    }

    for (var appNode : opSig.getApplicationNodes()) {
      var indexNode =
          ValidationUtils.validateNodeReference(
              graph,
              appNode.getIndexId(),
              IPFIndexNode.TYPE,
              IPFIndexNode.class,
              new LazyString(
                  () -> JsonPathUtils.concatJsonPath(appNode.getJsonPath(), "body.indexId")),
              issueCollector,
              lazyContexts);
      if (indexNode == null) {
        return;
      }

      validateProjectionAgreement(
          indexNode,
          "inputs",
          appNode.getInputs(),
          ipfSignatureNode.getInputs(),
          issueCollector,
          lazyContexts);
      validateProjectionAgreement(
          indexNode,
          "outputs",
          appNode.getOutputs(),
          ipfSignatureNode.getOutputs(),
          issueCollector,
          lazyContexts);
    }
  }

  @SuppressWarnings("unused")
  private void validateProjectionAgreement(
      IPFIndexNode opIndex,
      String selectionMapName,
      Map<String, List<TensorSelection>> selectionMap,
      Map<String, List<IndexProjectionFunction>> projectionMap,
      ValidationIssueCollector issueCollector,
      Supplier<List<ValidationIssue.Context>> lazyContexts) {
    if (!selectionMap.keySet().equals(projectionMap.keySet())) {
      issueCollector.addIssue(
          ValidationIssue.builder()
              .type(LoomConstants.NODE_VALIDATION_ERROR)
              .param("selectionMapName", selectionMapName)
              .param("selectionMapKeys", selectionMap.keySet())
              .param("projectionMapKeys", projectionMap.keySet())
              .summary("Selection map and projection map have different keys")
              .withContexts(lazyContexts));
      return;
    }

    var index = opIndex.getRange();

    for (var entry : selectionMap.entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();
      final var projections = projectionMap.get(ioName);

      if (selections.size() != projections.size()) {
        issueCollector.addIssue(
            ValidationIssue.builder()
                .type(LoomConstants.NODE_VALIDATION_ERROR)
                .param("selectionMapName", selectionMapName)
                .param("ioName", ioName)
                .param("selections", selections)
                .param("projections", projections)
                .summary("Selection map and projection map have different sizes")
                .withContexts(lazyContexts));
        continue;
      }

      for (int idx = 0; idx < selections.size(); ++idx) {
        var selection = selections.get(idx);
        var projection = projections.get(idx);

        var expected = projection.apply(index);

        ZRange projectedRange = selection.getRange();
        if (!projectedRange.equals(expected)) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .param("selectionMapName", selectionMapName)
                  .param("ioName", ioName)
                  .param("selection", selection)
                  .param("projection", projection)
                  .param("expected", expected)
                  .summary("Selection map and projection map have different ranges")
                  .withContexts(lazyContexts));
        }
      }
    }
  }
}
