package loom.graph;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;

public interface TConsumesInputsProperty extends TNodeInterface {
  @CanIgnoreReturnValue
  default TConsumesEdge bindInput(TTensor tensor, String label) {
    return assertGraph().addNode(new TConsumesEdge(getId(), tensor.id, label));
  }

  default List<TConsumesEdge> getInputEdges() {
    return assertGraph().queryEdges(TConsumesEdge.class).withSourceId(getId()).toList();
  }
}
