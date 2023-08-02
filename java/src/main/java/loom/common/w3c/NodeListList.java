package loom.common.w3c;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import java.util.AbstractList;
import java.util.List;

/** A {@link List<Node>} implementation backed by a {@link NodeList}. */
@AllArgsConstructor
public final class NodeListList extends AbstractList<Node> implements NodeList {

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
