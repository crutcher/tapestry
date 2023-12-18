package loom.graph.nodes;

import static loom.graph.LoomConstants.NODE_VALIDATION_ERROR;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import loom.common.json.JsonPathUtils;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

public class OperationNodesSourcesAndResultsAreTensors implements LoomEnvironment.Constraint {
  @Override
  public void check(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {

    for (var opNode : graph.iterableNodes(OperationNode.TYPE, OperationNode.class)) {
      checkOperation(opNode, issueCollector);
    }
  }

  static void checkIOMap(
      OperationNode opNode,
      String field,
      Map<String, List<UUID>> ioMap,
      ValidationIssueCollector issueCollector,
      List<ValidationIssue.Context> contexts) {
    final var graph = opNode.assertGraph();

    for (var entry : ioMap.entrySet()) {
      final var ioName = entry.getKey();
      final var refIds = entry.getValue();

      Supplier<String> description = () -> "Operation .%s/%s".formatted(field, ioName);

      for (int idx = 0; idx < refIds.size(); ++idx) {
        var refId = refIds.get(idx);

        Function<Integer, ValidationIssue.Context> reference =
            (i) ->
                ValidationIssue.Context.builder()
                    .name("Reference")
                    .jsonpath(
                        JsonPathUtils.concatJsonPath(
                            opNode.getJsonPath(), field, "%s[%d]".formatted(ioName, i)))
                    .dataFromTree(refId.toString())
                    .build();

        if (!graph.hasNode(refId)) {
          ValidationIssue.ValidationIssueBuilder issue =
              ValidationIssue.builder()
                  .type(NODE_VALIDATION_ERROR)
                  .param("nodeType", OperationNode.TYPE)
                  .summary("%s references nonexistent node".formatted(description.get()))
                  .context(reference.apply(idx));

          contexts.forEach(issue::context);

          issueCollector.add(issue);
          continue;
        }

        var ioNode = graph.assertNode(refId);
        if (!(ioNode instanceof TensorNode)) {
          var issue =
              ValidationIssue.builder()
                  .type(NODE_VALIDATION_ERROR)
                  .param("nodeType", OperationNode.TYPE)
                  .summary("%s references non-tensor node %s".formatted(description.get(), refId))
                  .context(reference.apply(idx))
                  .context(
                      ValidationIssue.Context.builder()
                          .name("Reference Node")
                          .jsonpath(ioNode.getJsonPath())
                          .dataFromTree(ioNode)
                          .build());

          contexts.forEach(issue::context);
          issueCollector.add(issue);
        }
      }
    }
  }

  @VisibleForTesting
  static void checkOperation(OperationNode opNode, ValidationIssueCollector issueCollector) {
    List<ValidationIssue.Context> contexts =
        List.of(
            ValidationIssue.Context.builder()
                .name("Operation")
                .jsonpath(opNode.getJsonPath())
                .dataFromTree(opNode)
                .build());

    checkIOMap(opNode, "inputs", opNode.getInputs(), issueCollector, contexts);
    checkIOMap(opNode, "outputs", opNode.getOutputs(), issueCollector, contexts);
  }
}
