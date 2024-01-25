package org.tensortapestry.loom.graph;

import java.util.Set;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.common.json.JsonSchemaFactoryManager;
import org.tensortapestry.loom.graph.dialects.common.CommonNodes;
import org.tensortapestry.loom.graph.dialects.common.GenericNode;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.common.SchemaTypeConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.zspace.ZRange;

@UtilityClass
public final class CommonEnvironments {

  public static JsonSchemaFactoryManager buildJsonSchemaFactoryManager() {
    return new JsonSchemaFactoryManager()
      .bindResourcePath(
        "http://tensortapestry.org/schemas",
        LoomConstants.LOOM_SCHEMA_RESOURCES.getPath()
      );
  }

  public static LoomEnvironment genericEnvironment() {
    return LoomEnvironment
      .builder()
      .jsonSchemaFactoryManager(buildJsonSchemaFactoryManager())
      .defaultNodeTypeClass(GenericNode.class)
      .build()
      .addConstraint(new SchemaTypeConstraint());
  }

  public static LoomEnvironment expressionEnvironment() {
    return LoomEnvironment
      .builder()
      .jsonSchemaFactoryManager(buildJsonSchemaFactoryManager())
      .build()
      .addConstraint(new SchemaTypeConstraint())
      .addUrlAlias(LoomConstants.LOOM_NODE_TYPES_SCHEMA, "loom")
      .addUrlAlias(LoomConstants.LOOM_ANNOTATION_TYPES_SCHEMA, "loom")
      .autowireNodeTypeClass(CommonNodes.NOTE_NODE_TYPE, NoteNode.class)
      .autowireNodeTypeClass(TensorOpNodes.TENSOR_NODE_TYPE, TensorNode.class)
      .addConstraint(
        TensorDTypesAreValidConstraint.builder().validDTypes(Set.of("int32", "float32")).build()
      )
      .autowireNodeTypeClass(TensorOpNodes.APPLICATION_NODE_TYPE, ApplicationNode.class)
      .autowireNodeTypeClass(
        TensorOpNodes.OPERATION_SIGNATURE_NODE_TYPE,
        OperationSignatureNode.class
      )
      .addAnnotationTypeClass(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE, IPFSignature.class)
      .addAnnotationTypeClass(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE, ZRange.class);
  }
}
