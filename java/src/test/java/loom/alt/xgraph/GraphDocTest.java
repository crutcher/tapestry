package loom.alt.xgraph;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class GraphDocTest implements CommonAssertions {
  @Test
  public void testCreate() {
    var doc = GraphDoc.create();
    doc.validate();
  }

  @Test
  public void testFrom() {
    var graph =
        GraphDoc.from(
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <t:graph
                      xmlns:t="http://loom.org/v1"
                        id="00000000-0000-0000-0000-0000000000AA"
                      >
                      <t:node id="00000000-0000-0000-0000-0000000000E0" />
                      <t:node id="00000000-0000-0000-0000-0000000000E1" />
                      </t:graph>
                    """);

    assertThat(
            graph.nodes().stream()
                .map(n -> n.getAttributes().getNamedItem("id").getNodeValue())
                .toList())
        .contains("00000000-0000-0000-0000-0000000000E0", "00000000-0000-0000-0000-0000000000E1");
  }
}
