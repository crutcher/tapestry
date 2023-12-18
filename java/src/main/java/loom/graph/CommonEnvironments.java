package loom.graph;

import java.util.Collection;
import java.util.Set;
import loom.graph.nodes.*;

public final class CommonEnvironments {
  public static LoomEnvironment simpleTensorEnvironment(String... dtypes) {
    return simpleTensorEnvironment(Set.of(dtypes));
  }

  public static LoomEnvironment expressionEnvironment() {
    return simpleTensorEnvironment("int32", "float32")
        .addConstraint(new AllTensorsHaveExactlyOneSourceOperationConstraint())
        .addConstraint(new OperationNodesSourcesAndResultsAreTensors());
  }

  public static LoomEnvironment simpleTensorEnvironment(Collection<String> dtypes) {
    return LoomEnvironment.builder()
        .nodeMetaFactory(
            TypeMapNodeMetaFactory.builder()
                .typeMapping(
                    TensorNode.TYPE, TensorNode.Prototype.builder().validDTypes(dtypes).build())
                .typeMapping(OperationNode.TYPE, OperationNode.Prototype.builder().build())
                .typeMapping(NoteNode.TYPE, NoteNode.Prototype.builder().build())
                .build())
        .build();
  }

  private CommonEnvironments() {}
}
