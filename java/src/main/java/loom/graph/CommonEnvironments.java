package loom.graph;

import java.util.Set;
import lombok.NoArgsConstructor;
import loom.graph.constraints.*;
import loom.graph.nodes.*;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CommonEnvironments {
  public static LoomEnvironment simpleTensorEnvironment(String... dtypes) {
    return simpleTensorEnvironment(Set.of(dtypes));
  }

  public static LoomEnvironment simpleTensorEnvironment(Set<String> dtypes) {
    return LoomEnvironment.builder()
        .nodeMetaFactory(
            TypeMapNodeMetaFactory.builder()
                .typeMapping(TensorNode.TYPE, TensorNode.Prototype.builder().build())
                .typeMapping(OperationNode.TYPE, OperationNode.Prototype.builder().build())
                .typeMapping(ApplicationNode.TYPE, ApplicationNode.Prototype.builder().build())
                .typeMapping(NoteNode.TYPE, NoteNode.Prototype.builder().build())
                .build())
        .build()
        .addConstraint(TensorDTypesAreValidConstraint.builder().validDTypes(dtypes).build());
  }

  public static LoomEnvironment expressionEnvironment() {
    return simpleTensorEnvironment("int32", "float32")
        .addConstraint(new OperationNodesSourcesAndResultsAreTensors())
        .addConstraint(new AllTensorsHaveExactlyOneSourceOperationConstraint())
        .addConstraint(new ThereAreNoTensorOperationReferenceCyclesConstraint())
        .addConstraint(new ThereAreNoApplicationReferenceCyclesConstraint())
        .addConstraint(new ApplicationNodeSelectionsAreWellFormedConstraint());
  }
}
