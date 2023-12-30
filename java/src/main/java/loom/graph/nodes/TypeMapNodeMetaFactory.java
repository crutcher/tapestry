package loom.graph.nodes;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import loom.graph.LoomGraph;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public final class TypeMapNodeMetaFactory extends LoomGraph.NodeMetaFactory {
  @Singular private final Map<String, LoomGraph.NodePrototype<?, ?>> typeMappings;

  @Override
  public LoomGraph.NodePrototype<?, ?> getPrototypeForType(String type) {
    return typeMappings.get(type);
  }
}
