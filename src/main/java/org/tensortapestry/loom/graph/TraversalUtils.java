package org.tensortapestry.loom.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.jgrapht.Graph;
import org.jgrapht.alg.color.GreedyColoring;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.alg.interfaces.VertexColoringAlgorithm;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationBody;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorOpNodes;

@UtilityClass
public class TraversalUtils {

  /**
   * Find all simple cycles of Tensors and Operations in the graph.
   *
   * @param graph the graph to search
   * @return a list of cycles, where each cycle is a list of nodes in the cycle.
   */
  public List<List<LoomNode>> findOperationSimpleCycles(LoomGraph graph) {
    Graph<LoomNode, DefaultEdge> linkGraph = buildOpeartionLinkGraph(graph);

    List<List<LoomNode>> simpleCycles = new ArrayList<>();
    new TarjanSimpleCycles<>(linkGraph).findSimpleCycles(simpleCycles::add);
    // Tarjan will place all non-cycle nodes in their own cycles, so filter those out.
    return simpleCycles.stream().filter(cycle -> cycle.size() > 1).toList();
  }

  /**
   * Build a JGraphT graph of the data flow of Operation and Tensor nodes in the graph.
   *
   * <p>This is a directed graph where the nodes are Operation and Tensor nodes, and the edges
   * represent data flow from Tensor inputs to Operation nodes; and from Operation nodes to Tensor
   * outputs.
   *
   * @param graph the graph to traverse.
   * @return a JGraphT graph of the data flow.
   */
  @Nonnull
  public Graph<LoomNode, DefaultEdge> buildOpeartionLinkGraph(LoomGraph graph) {
    Graph<LoomNode, DefaultEdge> linkGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    for (var node : graph.byType(TensorOpNodes.OPERATION_NODE_TYPE)) {
      linkGraph.addVertex(node);
      var opData = node.viewBodyAs(OperationBody.class);

      for (var entry : opData.getInputs().entrySet()) {
        for (var tensorSelection : entry.getValue()) {
          var refNode = graph.assertNode(tensorSelection.getTensorId());
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(refNode, node);
        }
      }
      for (var entry : opData.getOutputs().entrySet()) {
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
   * Construct a coloring graph for tensor and operation adjacency.
   *
   * @param graph The graph to construct the coloring graph for.
   * @return The coloring graph.
   */
  public DefaultUndirectedGraph<UUID, DefaultEdge> tensorOperationColoringGraph(LoomGraph graph) {
    DefaultUndirectedGraph<UUID, DefaultEdge> coloringGraph = new DefaultUndirectedGraph<>(
      DefaultEdge.class
    );
    for (var opNode : graph.byType(TensorOpNodes.OPERATION_NODE_TYPE)) {
      var opId = opNode.getId();
      var opData = opNode.viewBodyAs(OperationBody.class);
      coloringGraph.addVertex(opId);

      List<UUID> tensorIds = new ArrayList<>();
      for (var entry : opData.getInputs().entrySet()) {
        for (var tensorSelection : entry.getValue()) {
          var tensorId = tensorSelection.getTensorId();
          tensorIds.add(tensorId);
          coloringGraph.addVertex(tensorId);
          coloringGraph.addEdge(opId, tensorId);
        }
      }
      for (var entry : opData.getOutputs().entrySet()) {
        for (var tensorSelection : entry.getValue()) {
          var tensorId = tensorSelection.getTensorId();
          tensorIds.add(tensorId);
          coloringGraph.addVertex(tensorId);
          coloringGraph.addEdge(opId, tensorId);
        }
      }

      for (int i = 0; i < tensorIds.size(); i++) {
        var a = tensorIds.get(i);
        for (int j = 0; j < i; j++) {
          coloringGraph.addEdge(a, tensorIds.get(j));
        }
      }
    }

    return coloringGraph;
  }

  /**
   * Color the nodes of a graph based on the adjacency of tensors and operations.
   *
   * @param graph The graph to color.
   * @return The Coloring.
   */
  public VertexColoringAlgorithm.Coloring<UUID> tensorOperationColoring(LoomGraph graph) {
    var coloringGraph = tensorOperationColoringGraph(graph);
    var greedyColoring = new GreedyColoring<>(coloringGraph);
    return greedyColoring.getColoring();
  }
}
