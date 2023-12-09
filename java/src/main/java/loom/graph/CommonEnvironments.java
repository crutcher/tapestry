package loom.graph;

import java.util.Collection;
import java.util.Set;
import loom.graph.nodes.TensorNode;
import loom.graph.nodes.TypeMapNodeMetaFactory;

public final class CommonEnvironments {
  public static LoomEnvironment simpleTensorEnvironment(String... dtypes) {
    return simpleTensorEnvironment(Set.of(dtypes));
  }

  public static LoomEnvironment simpleTensorEnvironment(Collection<String> dtypes) {
    return LoomEnvironment.builder()
        .nodeMetaFactory(
            TypeMapNodeMetaFactory.builder()
                .typeMapping(
                    TensorNode.Meta.TYPE, TensorNode.Meta.builder().validDTypes(dtypes).build())
                .build())
        .build();
  }

  private CommonEnvironments() {}
}
