package loom.graph;

import java.util.Collection;
import java.util.Set;
import loom.graph.nodes.OperationNode;
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
                    TensorNode.TYPE, TensorNode.Prototype.builder().validDTypes(dtypes).build())
                .typeMapping(
                    OperationNode.Prototype.TYPE, OperationNode.Prototype.builder().build())
                .build())
        .build();
  }

  private CommonEnvironments() {}
}
