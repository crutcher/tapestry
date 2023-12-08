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
  @Singular private final Map<String, LoomGraph.NodeMeta<?, ?>> typeMappings;

  @Override
  public LoomGraph.NodeMeta<?, ?> getMetaForType(String type) {
    return typeMappings.get(type);
  }
}
