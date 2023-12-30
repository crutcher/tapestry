package loom.graph;

import java.util.Set;
import lombok.NoArgsConstructor;
import loom.graph.constraints.*;
import loom.graph.nodes.*;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CommonEnvironments {
  public static LoomEnvironment genericEnvironment() {
    return LoomEnvironment.builder().defaultNodeTypeClass(GenericNode.class).build();
  }

  public static LoomEnvironment simpleTensorEnvironment(String... dtypes) {
    return simpleTensorEnvironment(Set.of(dtypes));
  }

  public static LoomEnvironment simpleTensorEnvironment(Set<String> dtypes) {
    return LoomEnvironment.builder()
        .nodeTypeClass(NoteNode.TYPE, NoteNode.class)
        .nodeTypeClass(TensorNode.TYPE, TensorNode.class)
        .nodeTypeClass(OperationNode.TYPE, OperationNode.class)
        .nodeTypeClass(ApplicationNode.TYPE, ApplicationNode.class)
        .build()
        .addConstraint(
            NodeBodySchemaConstraint.builder()
                .nodeType(TensorNode.TYPE)
                .withSchemaFrom(TensorNode.Body.class)
                .build())
        .addConstraint(
            NodeBodySchemaConstraint.builder()
                .nodeType(ApplicationNode.TYPE)
                .withSchemaFrom(ApplicationNode.Body.class)
                .build())
        .addConstraint(
            NodeBodySchemaConstraint.builder()
                .nodeType(NoteNode.TYPE)
                .withSchemaFrom(NoteNode.Body.class)
                .build())
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
