package loom.graph.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class ThereAreNoTensorOperationReferenceCyclesConstraint
    implements LoomEnvironment.Constraint {

  @Override
  public void check(LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector) {
    // Assuming TensorNode::AllTensorsHaveExactlyOneSourceOperation has already been run;
    // verify that there are no cycles in the graph.

    Graph<LoomGraph.Node<?, ?>, DefaultEdge> linkGraph =
        new DefaultDirectedGraph<>(DefaultEdge.class);

    for (var opNode : graph.iterableNodes(OperationNode.TYPE, OperationNode.class)) {
      linkGraph.addVertex(opNode);

      for (var entry : opNode.getInputNodes().entrySet()) {
        for (var refNode : entry.getValue()) {
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(refNode, opNode);
        }
      }

      for (var entry : opNode.getOutputNodes().entrySet()) {
        for (var refNode : entry.getValue()) {
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(opNode, refNode);
        }
      }
    }

    List<List<LoomGraph.Node<?, ?>>> simpleCycles = new ArrayList<>();
    new TarjanSimpleCycles<>(linkGraph).findSimpleCycles(simpleCycles::add);
    var cycles = simpleCycles.stream().filter(cycle -> cycle.size() > 1).toList();

    for (var cycle : cycles) {
      var cycleDesc =
          cycle.stream()
              .map(
                  item -> {
                    var desc = new HashMap<>();
                    desc.put("id", item.getId());
                    desc.put("type", item.getType());
                    if (item.getLabel() != null) {
                      desc.put("label", item.getLabel());
                    }
                    return desc;
                  })
              .toList();

      issueCollector.add(
          ValidationIssue.builder()
              .type(LoomConstants.REFERENCE_CYCLE_ERROR)
              .summary("Reference Cycle detected")
              .context(b -> b.name("Cycle").data(cycleDesc)));
    }
  }
}
