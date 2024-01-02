package loom.graph;

import java.util.Set;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import loom.graph.constraints.NodeBodySchemaConstraint;
import loom.graph.constraints.TensorDTypesAreValidConstraint;
import loom.graph.nodes.*;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CommonEnvironments {
  public static LoomEnvironment genericEnvironment() {
    return LoomEnvironment.builder()
        .defaultNodeTypeClass(GenericNode.class)
        .build()
        .addConstraint(
            NodeBodySchemaConstraint.builder()
                .nodeTypePattern(Pattern.compile("^.*$"))
                .withSchemaFromBodyClass(GenericNode.Body.class)
                .build());
  }

  public static LoomEnvironment expressionEnvironment() {
    return LoomEnvironment.builder()
        .build()
        .autowireNodeTypeClass(NoteNode.TYPE, NoteNode.class)
        .autowireNodeTypeClass(TensorNode.TYPE, TensorNode.class)
        .addConstraint(
            TensorDTypesAreValidConstraint.builder()
                .validDTypes(Set.of("int32", "float32"))
                .build())
        .autowireNodeTypeClass(ApplicationNode.TYPE, ApplicationNode.class)
        .autowireNodeTypeClass(OperationSignatureNode.TYPE, OperationSignatureNode.class)
        .autowireNodeTypeClass(IPFSignatureNode.TYPE, IPFSignatureNode.class);
  }
}
