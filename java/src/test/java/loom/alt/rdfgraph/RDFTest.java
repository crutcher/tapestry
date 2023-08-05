package loom.alt.rdfgraph;

import loom.testing.CommonAssertions;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Test;

public class RDFTest implements CommonAssertions {
  public static final String NS = "http://loom.com/v1#";

  public static void dumpModel(Model model, String format) {
    System.out.println("Format: " + format);
    model.write(System.out, format);
    System.out.println();
  }

  @Test
  public void testRdfBasic() {
    Model model = ModelFactory.createDefaultModel();
    model.setNsPrefix("loom", NS);

    Resource bar = model.createResource("#bar");
    bar.addProperty(
        model.createProperty(NS, "shape"),
        model.createList(model.createTypedLiteral(1), model.createTypedLiteral(2)));

    Resource foo = model.createResource("#foo");
    foo.addProperty(
        model.createProperty(NS, "shape"),
        model.createList(model.createTypedLiteral(10), model.createTypedLiteral(2)));

    // for (var fmt : List.of("RDF/XML", "TURTLE", "N-TRIPLE", "JSON-LD-11")) {
    //   dumpModel(model, fmt);
    //   }
  }
}
