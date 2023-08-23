package loom.common.xml.w3c;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import loom.testing.CommonAssertions;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListListTest implements CommonAssertions {
  public static NodeList exampleNodeList() {
    try {
      var dbFactory = DocumentBuilderFactory.newInstance();
      dbFactory.setNamespaceAware(true);
      var dBuilder = dbFactory.newDocumentBuilder();

      String source =
          """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <doc>
                      <node id="0"/>
                      <node id="1"/>
                      <node id="2"/>
                      <node id="3"/>
                      <node id="4"/>
                      </doc>
                    """;

      var doc = dBuilder.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
      return doc.getDocumentElement().getElementsByTagName("node");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testBasic() {
    var source = exampleNodeList();
    var wrapper = new NodeListList(source);

    // NodeList delegates.
    assertThat(wrapper.item(3)).isEqualTo(source.item(3));
    assertThat(wrapper.getLength()).isEqualTo(source.getLength());

    assertThat(wrapper.get(3)).isEqualTo(source.item(3));
    assertThat(wrapper.size()).isEqualTo(source.getLength());

    assertThat(wrapper.isEmpty()).isFalse();

    var node = source.item(2);
    assertThat(wrapper.contains(node)).isTrue();
    assertThat(wrapper.indexOf(node)).isEqualTo(2);
    assertThat(wrapper.lastIndexOf(node)).isEqualTo(2);

    assertThat(wrapper.get(0)).isSameAs(source.item(0));

    assertThat(wrapper.containsAll(List.of(wrapper.get(0), wrapper.get(1)))).isTrue();

    assertThat(wrapper.toArray()).isEqualTo(new ArrayList<>(wrapper).toArray());

    assertThat(wrapper.toArray(new Node[0]))
        .isEqualTo(new ArrayList<>(wrapper).toArray(new Node[0]));

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> wrapper.add(null));
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> wrapper.remove(1));
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> wrapper.remove(wrapper.get(0)));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> wrapper.listIterator().set(null));
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> wrapper.listIterator().add(null));
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> wrapper.listIterator().remove());
  }

  @Test
  public void testAddNode() {
    var source = exampleNodeList();
    var wrapper = new NodeListList(source);

    source.item(0).getParentNode().appendChild(source.item(0).cloneNode(true));

    assertThat(wrapper).hasSize(6);

    assertThat(
            wrapper.stream().map(n -> n.getAttributes().getNamedItem("id").getNodeValue()).toList())
        .contains("0", "1", "2", "3", "4", "0");
  }

  @Test
  public void testSublist() {
    var source = exampleNodeList();
    var wrapper = new NodeListList(source);

    var sub = wrapper.subList(1, 3);
    assertThat(sub).hasSize(2).contains(source.item(1), source.item(2));
  }
}
