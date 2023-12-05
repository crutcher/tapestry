package loom.doozer;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public final class TypeMapNodeMetaFactory extends DoozerGraph.Node.NodeMetaFactory {
  @Singular private final Map<String, DoozerGraph.Node.NodeMeta<?, ?>> typeMappings;

  @Override
  public DoozerGraph.Node.NodeMeta<?, ?> getMeta(String type) {
    return typeMappings.get(type);
  }
}
