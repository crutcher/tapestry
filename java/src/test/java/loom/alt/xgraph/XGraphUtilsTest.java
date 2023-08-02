package loom.alt.xgraph;

import loom.testing.CommonAssertions;
import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;

public class XGraphUtilsTest implements CommonAssertions {
  @Test
  public void testGetSchema() {
    assertNotNull(XGraphUtils.getSchema());
  }

  @Test
  public void testValidateXGraph() throws Exception {
    var dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true);
    var dBuilder = dbFactory.newDocumentBuilder();

    String source =
        """
                <?xml version="1.0" encoding="UTF-8"?>
                <t:graph
                  xmlns:t="http://loom.org/v1"
                    id="00000000-0000-0000-0000-0000000000AA"
                  >
                  <t:node id="00000000-0000-0000-0000-0000000000E0" />
                  <t:node id="00000000-0000-0000-0000-0000000000E1" />

                  </t:graph>
                """;

    @SuppressWarnings("unused")
    var doc = dBuilder.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));

    var schema = XGraphUtils.getSchema();
    var validator = schema.newValidator();

    validator.validate(new DOMSource(doc));

    var nodes = XGraphUtils.graphNodes(doc);
    nodes.forEach(System.out::println);
  }
}
