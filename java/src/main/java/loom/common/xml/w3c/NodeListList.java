package loom.common.xml.w3c;

import java.util.AbstractList;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** A {@link List<Node>} implementation backed by a {@link NodeList}. */
@AllArgsConstructor
public final class NodeListList extends AbstractList<Node> implements NodeList {
  @AllArgsConstructor
  public static class NamedNodeMapAdapter implements NodeList {
    private final NamedNodeMap namedNodeMap;

    @Override
    public Node item(int index) {
      return namedNodeMap.item(index);
    }

    @Override
    public int getLength() {
      return namedNodeMap.getLength();
    }
  }

  public static NodeListList of(NamedNodeMap nodeMap) {
    return new NodeListList(new NamedNodeMapAdapter(nodeMap));
  }

  public static NodeListList of(NodeList nodeList) {
    return new NodeListList(nodeList);
  }

  @Delegate @Nonnull private final NodeList nodeList;

  @Override
  public int size() {
    return getLength();
  }

  @Override
  public Node get(int index) {
    return item(index);
  }
}
