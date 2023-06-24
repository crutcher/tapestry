package loom.graph.export;

import loom.graph.TGraph;
import loom.graph.THappensAfterEdge;
import loom.graph.TSequencePoint;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class TGraphDotExporterTest implements CommonAssertions {
  @Test
  public void test_to_graph() {
    var graph = new TGraph();
    var sp1 = graph.addNode(new TSequencePoint());
    var sp2 = graph.addNode(new TSequencePoint());
    graph.addNode(new THappensAfterEdge(sp2.id, sp1.id));

    @SuppressWarnings("unused")
    var dot = TGraphDotExporter.builder().build().toGraph(graph).toString();

    @SuppressWarnings("unused")
    var img = new TGraphDotExporter().toImage(graph);
  }
}
