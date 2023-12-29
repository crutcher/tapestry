package loom.graph;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.NoArgsConstructor;
import loom.graph.nodes.ApplicationNode;
import loom.graph.nodes.OperationNode;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

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

  /**
   * Build a graph of the links between Tensors and Operations in the graph.
   *
   * @param graph the graph to build the link graph for.
   * @return a JGraphT graph of the links between Tensors and Operations in the graph.
   */
  @Nonnull
  public static Graph<LoomGraph.Node<?, ?>, DefaultEdge> buildOperationLinkGraph(LoomGraph graph) {
    Graph<LoomGraph.Node<?, ?>, DefaultEdge> linkGraph =
        new DefaultDirectedGraph<>(DefaultEdge.class);

    for (var opNode : graph.iterableNodes(OperationNode.TYPE, OperationNode.class)) {
      linkGraph.addVertex(opNode);

      for (var entry : opNode.getInputNodeListMap().entrySet()) {
        for (var refNode : entry.getValue()) {
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(refNode, opNode);
        }
      }

      for (var entry : opNode.getOutputNodeListMap().entrySet()) {
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
  public static List<List<LoomGraph.Node<?, ?>>> findOperationSimpleCycles(LoomGraph graph) {
    Graph<LoomGraph.Node<?, ?>, DefaultEdge> linkGraph = buildOperationLinkGraph(graph);

    List<List<LoomGraph.Node<?, ?>>> simpleCycles = new ArrayList<>();
    new TarjanSimpleCycles<>(linkGraph).findSimpleCycles(simpleCycles::add);
    // Tarjan will place all non-cycle nodes in their own cycles, so filter those out.
    return simpleCycles.stream().filter(cycle -> cycle.size() > 1).toList();
  }
}
