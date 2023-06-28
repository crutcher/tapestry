package loom.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import loom.common.LookupError;

@JsonTypeName("Observer")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "Mcircle"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "#B4F8C8"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "margin", value = "0")
    })
public final class TObserver extends TSequencePoint {
  public TObserver(@Nullable UUID id) {
    super(id);
  }

  public TObserver() {
    this((UUID) null);
  }

  public TObserver(@Nonnull TObserver source) {
    this(source.id);
  }

  @Override
  public TObserver copy() {
    return new TObserver(this);
  }

  public Set<TNodeInterface> findObservedNodes() {
    var g = assertGraph();
    Set<TNodeInterface> observed = new HashSet<>();
    List<TNodeInterface> scheduled = new ArrayList<>();
    scheduled.add(this);
    while (!scheduled.isEmpty()) {
      var node = scheduled.remove(0);
      if (observed.contains(node)) {
        continue;
      }
      observed.add(node);

      if (node instanceof TEdgeBase<?, ?> tedge) {
        scheduled.add(tedge.getTarget());
      }

      g.queryTags(TTagBase.class).withSourceId(node.getId()).forEach(scheduled::add);
    }
    return observed;
  }

  @Override
  public void validate() {
    super.validate();
    var g = assertGraph();
    var observers = g.queryNodes(TObserver.class).toList();
    if (observers.size() > 1) {
      throw new LookupError("Multiple observers found in graph: " + observers);
    }
  }
}
