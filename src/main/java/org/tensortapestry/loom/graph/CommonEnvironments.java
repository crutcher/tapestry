package org.tensortapestry.loom.graph;

import java.util.Set;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.TensorDTypesAreValidConstraint;
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
}
