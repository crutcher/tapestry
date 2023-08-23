package loom.common.xml.w3c;

import java.util.*;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public final class XmlUtils {
  private XmlUtils() {}

  public static Map<String, String> documentNamespaceMap(Document doc) {
    return NodeListList.of(doc.getDocumentElement().getAttributes()).stream()
        .filter(n -> n.getNodeName().startsWith("xmlns:"))
        .collect(Collectors.toMap(Node::getLocalName, Node::getNodeValue));
  }
}
