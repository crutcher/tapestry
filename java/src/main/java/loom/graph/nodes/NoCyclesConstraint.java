package loom.graph.nodes;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import loom.graph.GraphConstraint;
import loom.graph.LoomGraph;
import loom.graph.LoomGraphEnv;
import loom.graph.validation.ValidationIssue;
import loom.graph.validation.ValidationIssueCollector;

public class NoCyclesConstraint implements GraphConstraint {

  @Data
  @AllArgsConstructor
  public static class CycleChecker {
    private final LoomGraphEnv env;
    private final LoomGraph graph;
    private final ValidationIssueCollector issueCollector;
    private final Map<UUID, OperationNodeTypeBindings.OperationFields> opToFields;
    private final Map<UUID, UUID> tensorToProducer;

    public CycleChecker(
        LoomGraphEnv env, LoomGraph graph, ValidationIssueCollector issueCollector) {
      this.env = env;
      this.graph = graph;
      this.issueCollector = issueCollector;

      opToFields = new HashMap<>();
      tensorToProducer = new HashMap<>();
      for (var opNode : graph.nodes(OperationNodeTypeBindings.OPERATION_TYPE)) {
        var fields = OperationNodeTypeBindings.parseFields(opNode);
        opToFields.put(opNode.getId(), fields);

        for (var output : fields.getOutputs().entrySet()) {
          for (var tensorId : output.getValue()) {
            tensorToProducer.put(tensorId, opNode.getId());
          }
        }
      }
    }

    public String formatCycle(List<UUID> cycle) {
      var steps = new ArrayList<String>();
      for (var id : cycle) {
        var description = id.toString();
        steps.add(description);
      }

      return String.join(">", steps);
    }

    public void checkTensor(UUID tensorId, UUID start, List<UUID> path) {
      var newPath = new ArrayList<UUID>();
      newPath.add(tensorId);
      newPath.addAll(path);

      if (path.contains(start)) {
        issueCollector.add(
            ValidationIssue.builder("Cycle")
                .summary("Tensor %s is part of a cycle".formatted(tensorId))
                .message("Full cycle: %s".formatted(formatCycle(newPath)))
                .context(
                    ValidationIssue.Context.builder().name("Cycle").dataFromTree(newPath).build()));
        return;
      }

      var producerId = tensorToProducer.get(tensorId);
      var producerFields = opToFields.get(producerId);

      for (var input : producerFields.getInputs().entrySet()) {
        for (var inputTensorId : input.getValue()) {
          checkTensor(inputTensorId, start, newPath);
        }
      }
    }
  }

  @Override
  public void validate(LoomGraphEnv env, LoomGraph graph, ValidationIssueCollector issueCollector) {
    var cycleChecker = new CycleChecker(env, graph, issueCollector);

    for (var tensorNode : graph.nodes(TensorNodeTypeBindings.TENSOR_TYPE)) {
      var tensorId = tensorNode.getId();
      cycleChecker.checkTensor(tensorId, tensorId, List.of());
    }
  }
}
