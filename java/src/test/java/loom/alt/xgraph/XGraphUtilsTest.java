package loom.alt.xgraph;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import loom.testing.CommonAssertions;
import org.junit.Test;

public class XGraphUtilsTest implements CommonAssertions {

  @Test
  public void testValidateXGraph() throws Exception {
    var dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true);
    var dBuilder = dbFactory.newDocumentBuilder();

    String source =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <eg:graph
              xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
          >
          <eg:nodes>
            <eg:tensor id="node-00000000-0000-0000-0000-0000000000E1">
              <eg:dtype>float32</eg:dtype>
              <eg:shape>
                <eg:dim size="30"/>
                <eg:dim size="2"/>
                </eg:shape>
              </eg:tensor>
          </eg:nodes>
          </eg:graph>
        """;

    var doc = dBuilder.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));

    var schema = XGraphUtils.getLoomSchema(doc);
    var validator = schema.newValidator();

    validator.validate(new DOMSource(doc));
  }
}
