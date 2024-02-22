package org.tensortapestry.loom.graph;

import java.util.Set;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.common.SchemaTypeConstraint;
import org.tensortapestry.loom.graph.dialects.common.TypeRestrictionConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.*;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorDTypesAreValidConstraint;
import org.tensortapestry.loom.json.JsonSchemaFactoryManager;

@UtilityClass
public final class CommonEnvironments {

  public static JsonSchemaFactoryManager buildJsonSchemaFactoryManager() {
    return new JsonSchemaFactoryManager()
      .bindResourcePath(
        "http://tensortapestry.org/schemas",
        LoomConstants.LOOM_SCHEMA_RESOURCES.getPath()
      );
  }

  public static JsonSchemaFactoryManager COMMON_SCHEMA_MANAGER = buildJsonSchemaFactoryManager();

  public static TensorDTypesAreValidConstraint commonDTypeConstraint() {
    return TensorDTypesAreValidConstraint.builder().validDTypes(Set.of("int32", "float32")).build();
  }

  public static final LoomEnvironment OPERATION_EXPRESSION_ENVIRONMENT = LoomEnvironment
    .builder()
    .typeSupportProvider(
      TypeRestrictionConstraint
        .builder()
        .nodeType(TensorNode.TYPE)
        .nodeType(OperationNode.TYPE)
        .tagType(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
        .tagType(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE)
        .tagType(TensorOpNodes.IO_SEQUENCE_POINT_TYPE)
        .build()
    )
    .jsonSchemaFactoryManager(COMMON_SCHEMA_MANAGER)
    .constraint(new SchemaTypeConstraint())
    .urlAlias("http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd", "loom")
    .urlAlias("http://tensortapestry.org/schemas/loom/2024-01/tag_types.jsd", "loom")
    .constraint(commonDTypeConstraint())
    .constraint(new TensorOperationAgreementConstraint())
    .constraint(new NoTensorOperationCyclesConstraint())
    .constraint(new OperationIPFSignatureAgreementConstraint())
    .build();

  public static final LoomEnvironment APPLICATION_EXPRESSION_ENVIRONMENT = LoomEnvironment
    .builder()
    .typeSupportProvider(
      TypeRestrictionConstraint
        .builder()
        .nodeType(NoteNode.TYPE)
        .nodeType(TensorNode.TYPE)
        .nodeType(OperationNode.TYPE)
        .nodeType(ApplicationNode.TYPE)
        .tagType(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
        .tagType(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE)
        .tagType(TensorOpNodes.IO_SEQUENCE_POINT_TYPE)
        .build()
    )
    .jsonSchemaFactoryManager(COMMON_SCHEMA_MANAGER)
    .constraint(new SchemaTypeConstraint())
    .urlAlias("http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd", "loom")
    .urlAlias("http://tensortapestry.org/schemas/loom/2024-01/tag_types.jsd", "loom")
    .constraint(commonDTypeConstraint())
    .constraint(new TensorOperationAgreementConstraint())
    .constraint(new NoTensorOperationCyclesConstraint())
    .constraint(new OperationIPFSignatureAgreementConstraint())
    .constraint(new OperationApplicationAgreementConstraint())
    .constraint(new ApplicationIPFSignatureAgreementConstraint())
    .constraint(new ApplicationOutputRangeCoverageIsExactConstraint())
    .build();
}
