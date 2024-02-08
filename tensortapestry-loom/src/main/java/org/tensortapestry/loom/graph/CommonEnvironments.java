package org.tensortapestry.loom.graph;

import java.util.Set;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.dialects.common.SchemaTypeConstraint;
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

  public static LoomEnvironment expressionEnvironment() {
    return LoomEnvironment
      .builder()
      .jsonSchemaFactoryManager(buildJsonSchemaFactoryManager())
      .build()
      .addConstraint(new SchemaTypeConstraint())
      .addUrlAlias("http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd", "loom")
      .addUrlAlias("http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd", "loom")
      .addConstraint(
        TensorDTypesAreValidConstraint.builder().validDTypes(Set.of("int32", "float32")).build()
      )
      .addConstraint(new OperationReferenceAgreementConstraint())
      .addConstraint(new IPFSignatureAgreementConstraint());
  }
}
