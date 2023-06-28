package loom.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TSourceKeyedEdge<C, S extends TNodeInterface, T extends TNodeInterface>
    extends TKeyedEdge<S, T> {
  public TSourceKeyedEdge(
      @Nullable UUID id, @Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String key) {
    super(id, sourceId, targetId, key);
  }

  public static <S extends TNodeInterface, K extends TSourceKeyedEdge<?, S, ?>>
      Map<String, K> collectMap(Class<K> clazz, S collector) {
    var items = collector.assertGraph().queryEdges(clazz).withSourceId(collector.getId()).toList();
    var map = new HashMap<String, K>();
    for (var item : items) {
      var key = item.getKey();
      if (map.containsKey(key)) {
        throw new IllegalStateException("Duplicate key: " + key);
      }
      map.put(item.getKey(), item);
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  public Map<String, C> collectMap() {
    return collectMap(getClass(), getSource());
  }

  @Override
  public void validate() {
    super.validate();
    collectMap();
  }

  @Override
  public Map<String, Object> displayData() {
    var data = super.displayData();

    if (collectMap().size() == 1) {
      data.remove("key");
    }

    return data;
  }
}
