package org.tensortapestry.loom.graph.dialects.tensorops;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.tensortapestry.common.json.JsonPathUtils;
import org.tensortapestry.common.lazy.LazyString;
import org.tensortapestry.common.lazy.Thunk;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.*;

public class TensorOperationAgreementConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertSupportsNodeType(TensorNode.TYPE);
    env.assertSupportsNodeType(OperationNode.TYPE);
  }

  @Override
  public void validateConstraint(
    @SuppressWarnings("unused") LoomEnvironment env,
    LoomGraph graph,
    ValidationIssueCollector issueCollector
  ) {
    for (var operation : graph.byType(OperationNode.class)) {
      validateOperationNode(graph, operation, issueCollector);
    }
  }

  private static boolean validateOperationNode(
    LoomGraph graph,
    OperationNode operation,
    ValidationIssueCollector issueCollector
  ) {
    boolean valid = true;
    var lazyContexts = Thunk.of(() -> List.of(operation.asValidationContext("Operation Node")));

    valid &=
      validateTensorSelectionMap(
        graph,
        operation.unwrap(),
        "inputs",
        operation.getInputs(),
        lazyContexts,
        issueCollector
      );
    valid &=
      validateTensorSelectionMap(
        graph,
        operation.unwrap(),
        "outputs",
        operation.getOutputs(),
        lazyContexts,
        issueCollector
      );

    if (!valid) {
      return false;
    }

    return valid;
  }

  private static boolean validateTensorSelectionMap(
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

        TensorNode tensor;
        {
          var tmp = ValidationUtils.validateNodeReference(
            graph,
            tensorSelection.getTensorId(),
            TensorNode.TYPE,
            itemPath,
            issueCollector,
            contextsSupplier
          );
          if (tmp == null) {
            valid = false;
            continue;
          }
          tensor = TensorNode.wrap(tmp);
        }
        var tensorRange = tensor.getRange();

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
              .param("nodeType", OperationNode.TYPE)
              .param("expectedDimensions", selectionRange.getNDim())
              .param("actualDimensions", tensorRange.getNDim())
              .summary("Tensor selection has the wrong number of dimensions")
              .context(lazySelectionRangeContext)
              .context(tensor.asValidationContext("Tensor Node"))
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
              .param("nodeType", OperationNode.TYPE)
              .summary("Tensor selection is out of bounds")
              .context(lazySelectionRangeContext)
              .context(tensor.asValidationContext("Tensor Node"))
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
