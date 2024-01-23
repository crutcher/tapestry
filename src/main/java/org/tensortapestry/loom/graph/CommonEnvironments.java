package org.tensortapestry.loom.graph;

import java.util.Set;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import org.tensortapestry.loom.graph.constraints.NodeBodySchemaConstraint;
import org.tensortapestry.loom.graph.constraints.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.graph.constraints.TypeSchemaConstraint;
import org.tensortapestry.loom.graph.nodes.*;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CommonEnvironments {

  public static LoomEnvironment genericEnvironment() {
    return LoomEnvironment
      .builder()
      .defaultNodeTypeClass(GenericNode.class)
      .build()
      .addConstraint(
        NodeBodySchemaConstraint
          .builder()
          .nodeTypePattern(Pattern.compile("^.*$"))
          .withSchemaFromBodyClass(GenericNode.Body.class)
          .build()
      );
  }

  public static LoomEnvironment expressionEnvironment() {
    return LoomEnvironment
      .builder()
      .build()
      .addUrlAlias(LoomConstants.LOOM_CORE_SCHEMA, "loom")
      .autowireNodeTypeClass(NoteNode.TYPE, NoteNode.class)
      .autowireNodeTypeClass(TensorNode.TYPE, TensorNode.class)
      .addConstraint(
        TypeSchemaConstraint
          .builder()
          .nodeTypeSchema(NoteNode.TYPE, CommonSchemas.NOTE_NODE_SCHEMA)
          .nodeTypeSchema(TensorNode.TYPE, CommonSchemas.TENSOR_NODE_SCHEMA)
          .nodeTypeSchema(ApplicationNode.TYPE, CommonSchemas.APPLICATION_NODE_SCHEMA)
          .nodeTypeSchema(
            OperationSignatureNode.TYPE,
            CommonSchemas.OPERATION_SIGNATURE_NODE_SCHEMA
          )
          .build()
      )
      .addConstraint(
        TensorDTypesAreValidConstraint.builder().validDTypes(Set.of("int32", "float32")).build()
      )
      .autowireNodeTypeClass(ApplicationNode.TYPE, ApplicationNode.class)
      .autowireNodeTypeClass(OperationSignatureNode.TYPE, OperationSignatureNode.class)
      .addAnnotationTypeClass(IPFSignature.ANNOTATION_TYPE, IPFSignature.class)
      .addAnnotationTypeClass(IPFIndex.ANNOTATION_TYPE, IPFIndex.class);
  }
}
