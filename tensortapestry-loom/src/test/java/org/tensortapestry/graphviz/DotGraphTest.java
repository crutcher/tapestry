package org.tensortapestry.graphviz;

import java.awt.*;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class DotGraphTest implements CommonAssertions {

  @Test
  public void test() {
    var g = new DotGraph();
    g.getAttributes().set(GraphvizAttribute.LABEL, "test \" \\ graph");

    g.getEdgeDefaults().set(GraphvizAttribute.ARROWHEAD, "normal");

    g.getNodeDefaults().set(GraphvizAttribute.COLOR, Color.CYAN);

    var n1 = g.createNode("abc");
    n1.setNote("foo\nbar");
    n1
      .set(GraphvizAttribute.LABEL, HtmlLabel.from("<b>Label</b>"))
      .set(GraphvizAttribute.SHAPE, "box")
      .set(GraphvizAttribute.HEIGHT, 2.0);

    var n2 = g.createNode("def");

    var e = g.createEdge(n1.port("jkl").withCompassDir(CompassDir.N), n2);
    e.set("label", "edge label");

    var foo = g.createCluster("foo");
    foo.getAttributes().set(GraphvizAttribute.PERIPHERIES, 2);
    // System.out.println(g);
  }
}
