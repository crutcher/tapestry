package loom.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.NoArgsConstructor;
import loom.graph.nodes.*;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CommonEnvironments {
  public static LoomEnvironment simpleTensorEnvironment(String... dtypes) {
    return simpleTensorEnvironment(Set.of(dtypes));
  }

  public static LoomEnvironment simpleTensorEnvironment(Collection<String> dtypes) {
    return LoomEnvironment.builder()
        .nodeMetaFactory(
            TypeMapNodeMetaFactory.builder()
                .typeMapping(
                    TensorNode.TYPE, TensorNode.Prototype.builder().validDTypes(dtypes).build())
                .typeMapping(OperationNode.TYPE, OperationNode.Prototype.builder().build())
                .typeMapping(NoteNode.TYPE, NoteNode.Prototype.builder().build())
                .build())
        .build();
  }

  public static LoomEnvironment expressionEnvironment() {
    return simpleTensorEnvironment("int32", "float32")
        .addConstraint(new OperationNodesSourcesAndResultsAreTensors())
        .addConstraint(new AllTensorsHaveExactlyOneSourceOperationConstraint())
        .addConstraint(new ThereAreNoTensorOperationReferenceCyclesConstraint());
  }

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

  /**
   * Find all simple cycles of Tensors and Operations in the graph.
   *
   * @param graph the graph to search
   * @return a list of cycles, where each cycle is a list of nodes in the cycle.
   */
  public static List<List<LoomGraph.Node<?, ?>>> findSimpleCycles(LoomGraph graph) {
    Graph<LoomGraph.Node<?, ?>, DefaultEdge> linkGraph = buildLinkGraph(graph);

    List<List<LoomGraph.Node<?, ?>>> simpleCycles = new ArrayList<>();
    new TarjanSimpleCycles<>(linkGraph).findSimpleCycles(simpleCycles::add);
    // Tarjan will place all non-cycle nodes in their own cycles, so filter those out.
    return simpleCycles.stream().filter(cycle -> cycle.size() > 1).toList();
  }
}
