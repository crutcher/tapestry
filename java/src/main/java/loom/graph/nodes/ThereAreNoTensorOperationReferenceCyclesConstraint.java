package loom.graph.nodes;

import loom.graph.LoomConstants;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThereAreNoTensorOperationReferenceCyclesConstraint
    implements LoomEnvironment.Constraint {

  /**
   * Build a graph of the links between Tensors and Operations in the graph.
   *
   * @param graph the graph to build the link graph for.
   * @return a JGraphT graph of the links between Tensors and Operations in the graph.
   */
  @NotNull public static Graph<LoomGraph.Node<?, ?>, DefaultEdge> buildLinkGraph(LoomGraph graph) {
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
    return linkGraph;
  }

  @Override
  public void check(LoomEnvironment env, LoomGraph graph, ValidationIssueCollector issueCollector) {
    // Assuming TensorNode::AllTensorsHaveExactlyOneSourceOperation has already been run;
    // verify that there are no cycles in the graph.
    checkForCycles(graph, issueCollector);
  }

  public static void checkForCycles(LoomGraph graph, ValidationIssueCollector issueCollector) {
    Graph<LoomGraph.Node<?, ?>, DefaultEdge> linkGraph = buildLinkGraph(graph);

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
