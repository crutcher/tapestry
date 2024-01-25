package org.tensortapestry.loom.graph;

import java.util.Set;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.common.json.JsonSchemaFactoryManager;
import org.tensortapestry.loom.graph.constraints.NodeBodySchemaConstraint;
import org.tensortapestry.loom.graph.constraints.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.graph.constraints.TypeSchemaConstraint;
import org.tensortapestry.loom.graph.nodes.*;

@UtilityClass
public final class CommonEnvironments {

  public static JsonSchemaFactoryManager buildJsonSchemaFactoryManager() {
    return new JsonSchemaFactoryManager()
      .bindResourcePath("http://tensortapestry.org/schemas", "org/tensortapestry/schemas");
  }

  public static LoomEnvironment genericEnvironment() {
    return LoomEnvironment
      .builder()
      .jsonSchemaFactoryManager(buildJsonSchemaFactoryManager())
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
      .jsonSchemaFactoryManager(buildJsonSchemaFactoryManager())
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
