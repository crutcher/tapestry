package loom.graph;

import lombok.NoArgsConstructor;
import loom.graph.constraints.ApplicationNodeSelectionsAreWellFormedConstraint;
import loom.graph.constraints.NodeBodySchemaConstraint;
import loom.graph.constraints.TensorDTypesAreValidConstraint;
import loom.graph.constraints.ThereAreNoApplicationReferenceCyclesConstraint;
import loom.graph.nodes.*;

import java.util.Set;

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
                .withSchemaFromBodyClass(GenericNode.Body.class)
                .build());
  }

  public static LoomEnvironment expressionEnvironment() {
    return LoomEnvironment.builder()
        .build()
        .registerNodeType(NoteNode.TYPE, NoteNode.class)
        .registerNodeType(TensorNode.TYPE, TensorNode.class)
        .addConstraint(
            TensorDTypesAreValidConstraint.builder()
                .validDTypes(Set.of("int32", "float32"))
                .build())
        .registerNodeType(OperationSignatureNode.TYPE, OperationSignatureNode.class)
        .registerNodeType(ApplicationNode.TYPE, ApplicationNode.class)
        .addConstraint(new ThereAreNoApplicationReferenceCyclesConstraint())
        .addConstraint(new ApplicationNodeSelectionsAreWellFormedConstraint());
  }
}
