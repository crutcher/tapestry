package loom.graph;

import lombok.NoArgsConstructor;
import loom.graph.nodes.ApplicationNode;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TraversalUtils {
  /**
   * Find all simple cycles of Tensors and Applications in the graph.
   *
   * @param graph the graph to search
   * @return a list of cycles, where each cycle is a list of nodes in the cycle.
   */
  public static List<List<LoomGraph.Node<?, ?>>> findApplicationSimpleCycles(LoomGraph graph) {
    Graph<LoomGraph.Node<?, ?>, DefaultEdge> linkGraph = buildApplicationLinkGraph(graph);

    List<List<LoomGraph.Node<?, ?>>> simpleCycles = new ArrayList<>();
    new TarjanSimpleCycles<>(linkGraph).findSimpleCycles(simpleCycles::add);
    // Tarjan will place all non-cycle nodes in their own cycles, so filter those out.
    return simpleCycles.stream().filter(cycle -> cycle.size() > 1).toList();
  }

  /**
   * Build a JGraphT graph of the data flow of Application and Tensor nodes in the graph.
   *
   * <p>This is a directed graph where the nodes are Application and Tensor nodes, and the edges
   * represent data flow from Tensor inputs to Application nodes; and from Application nodes to
   * Tensor outputs.
   *
   * @param graph the graph to traverse.
   * @return a JGraphT graph of the data flow.
   */
  @Nonnull
  public static Graph<LoomGraph.Node<?, ?>, DefaultEdge> buildApplicationLinkGraph(
      LoomGraph graph) {
    Graph<LoomGraph.Node<?, ?>, DefaultEdge> linkGraph =
        new DefaultDirectedGraph<>(DefaultEdge.class);

    for (var node : graph.iterableNodes(ApplicationNode.TYPE, ApplicationNode.class)) {
      linkGraph.addVertex(node);

      for (var entry : node.getInputs().entrySet()) {
        for (var tensorSelection : entry.getValue()) {
          var refNode = graph.assertNode(tensorSelection.getTensorId());
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(refNode, node);
        }
      }
      for (var entry : node.getOutputs().entrySet()) {
        for (var tensorSelection : entry.getValue()) {
          var refNode = graph.assertNode(tensorSelection.getTensorId());
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(node, refNode);
        }
      }
    }
    return linkGraph;
  }
}
