package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import loom.common.json.JsonPathUtils;
import loom.common.lazy.LazyString;
import loom.common.lazy.Thunk;
import loom.graph.LoomConstants;
import loom.graph.LoomConstraint;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

public final class ApplicationNodeSelectionsAreWellFormedConstraint implements LoomConstraint {
  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertNodeTypeClass(TensorNode.TYPE, TensorNode.class);
    env.assertNodeTypeClass(ApplicationNode.TYPE, ApplicationNode.class);
  }

  @Override
  public void validateConstraint(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {
    for (var node : graph.iterableNodes(ApplicationNode.TYPE, ApplicationNode.class)) {
      validateApplicationNode(graph, node, issueCollector);
    }
  }

  private static void validateApplicationNode(
      @SuppressWarnings("unused") LoomGraph graph,
      ApplicationNode node,
      ValidationIssueCollector issueCollector) {
    var lazyContexts = Thunk.of(() -> List.of(node.asContext("Application Node")));
    // TODO: check that the operationId is a valid OperationSignature.
    validateTensorSliceMap(graph, node, "inputs", node.getInputs(), lazyContexts, issueCollector);
    validateTensorSliceMap(graph, node, "outputs", node.getOutputs(), lazyContexts, issueCollector);
  }

  private static void validateTensorSliceMap(
      LoomGraph graph,
      ApplicationNode node,
      String sliceMapName,
      Map<String, List<ApplicationNode.TensorSelection>> selectionMap,
      Supplier<List<ValidationIssue.Context>> contextsSupplier,
      ValidationIssueCollector issueCollector) {
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
                  .param("nodeType", ApplicationNode.TYPE)
                  .param("expectedDimensions", selectionRange.getNDim())
                  .param("actualDimensions", tensorRange.getNDim())
                  .summary("Tensor selection has the wrong number of dimensions")
                  .context(lazySelectionRangeContext)
                  .context(tensorNode.asContext("Tensor Node"))
                  .withContexts(contextsSupplier)
                  .build());
          continue;
        }

        if (!tensorRange.contains(selectionRange)) {
          issueCollector.addIssue(
              ValidationIssue.builder()
                  .type(LoomConstants.NODE_VALIDATION_ERROR)
                  .param("nodeType", ApplicationNode.TYPE)
                  .summary("Tensor selection is out of bounds")
                  .context(lazySelectionRangeContext)
                  .context(tensorNode.asContext("Tensor Node"))
                  .withContexts(contextsSupplier)
                  .build());
        }
      }
    }
  }
}
