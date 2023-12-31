package loom.graph;

import java.util.Set;
import lombok.NoArgsConstructor;
import loom.graph.constraints.*;
import loom.graph.nodes.*;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CommonEnvironments {
  public static LoomEnvironment genericEnvironment() {
    return LoomEnvironment.builder()
        .defaultNodeTypeClass(GenericNode.class)
        .build()
        .addConstraint(
            NodeBodySchemaConstraint.builder()
                .nodeType("^.*$")
                .isRegex(true)
                .withSchemaFrom(GenericNode.Body.class)
                .build());
  }

  public static LoomEnvironment expressionEnvironment() {
    return LoomEnvironment.builder()
        .build()
        .addNodeTypeClass(NoteNode.TYPE, NoteNode.class)
        .addNodeTypeClass(TensorNode.TYPE, TensorNode.class)
        .addNodeTypeClass(OperationNode.TYPE, OperationNode.class)
        .addNodeTypeClass(ApplicationNode.TYPE, ApplicationNode.class)
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
        .addConstraint(
            TensorDTypesAreValidConstraint.builder()
                .validDTypes(Set.of("int32", "float32"))
                .build())
        .addConstraint(new OperationNodesSourcesAndResultsAreTensors())
        .addConstraint(new AllTensorsHaveExactlyOneSourceOperationConstraint())
        .addConstraint(new ThereAreNoTensorOperationReferenceCyclesConstraint())
        .addConstraint(new ThereAreNoApplicationReferenceCyclesConstraint())
        .addConstraint(new ApplicationNodeSelectionsAreWellFormedConstraint());
  }
}
