package loom.doozer;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public final class TypeMapNodeMetaFactory extends DoozerGraph.NodeMetaFactory {
  @Singular private final Map<String, DoozerGraph.NodeMeta<?, ?>> typeMappings;

  @Override
  public DoozerGraph.NodeMeta<?, ?> getMeta(String type) {
    return typeMappings.get(type);
  }
}
