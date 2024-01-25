package org.tensortapestry.loom.graph;

import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.common.runtime.ResourceHandle;

@SuppressWarnings("unused")
@UtilityClass
public class LoomConstants {

  public String LOOM_SCHEMA_BASE_URL = "https://tensortapestry.org/schemas/loom/2024-01/";

  public String LOOM_DATA_TYPES_SCHEMA = LOOM_SCHEMA_BASE_URL + "data_types.jsd";
  public String LOOM_NODE_TYPES_SCHEMA = LOOM_SCHEMA_BASE_URL + "node_types.jsd";
  public String LOOM_ANNOTATION_TYPES_SCHEMA = LOOM_SCHEMA_BASE_URL + "annotation_types.jsd";

  public ResourceHandle LOOM_SCHEMA_RESOURCES = new ResourceHandle("org/tensortapestry/schemas");

  public String LOOM_CORE_SCHEMA = "https://tensortapestry.org/schemas/loom/core.0.0.1.jsd";

  // TODO: Switch to lookup by anchor when this is fixed:
  // See: https://github.com/networknt/json-schema-validator/pull/930
  public Function<String, String> LOOM_CORE_NODE_TYPE = (String target) ->
    LOOM_NODE_TYPES_SCHEMA + "#/$defs/%s".formatted(target);

  public Function<String, String> LOOM_CORE_ANNOTATION_TYPE = (String target) ->
    LOOM_ANNOTATION_TYPES_SCHEMA + "#/$defs/%s".formatted(target);

  @UtilityClass
  public static class Errors {

    public String NODE_SCHEMA_ERROR = "NodeSchemaError";
    public String NODE_VALIDATION_ERROR = "NodeValidationError";
    public String NODE_REFERENCE_ERROR = "NodeReferenceError";
    public String REFERENCE_CYCLE_ERROR = "ReferenceCycle";
  }
}
