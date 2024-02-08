package org.tensortapestry.loom.graph;

import lombok.experimental.UtilityClass;
import org.tensortapestry.common.runtime.ResourceHandle;

@SuppressWarnings("unused")
@UtilityClass
public class LoomConstants {

  // TODO: Switch this to https once I unfuck the JsonSchemaFactory lookup machinery.
  public String LOOM_SCHEMA_BASE_URL = "http://tensortapestry.org/schemas/loom/2024-01/";

  public String LOOM_DATA_TYPES_SCHEMA = LOOM_SCHEMA_BASE_URL + "data_types.jsd";
  public String LOOM_NODE_TYPES_SCHEMA = LOOM_SCHEMA_BASE_URL + "node_types.jsd";
  public String LOOM_ANNOTATION_TYPES_SCHEMA = LOOM_SCHEMA_BASE_URL + "annotation_types.jsd";

  public ResourceHandle LOOM_SCHEMA_RESOURCES = new ResourceHandle("org/tensortapestry/schemas");

  @UtilityClass
  public static class Errors {

    public String JSD_ERROR = "JSD_ERROR";
    public String NODE_SCHEMA_ERROR = "NodeSchemaError";
    public String NODE_VALIDATION_ERROR = "NodeValidationError";
    public String NODE_REFERENCE_ERROR = "NodeReferenceError";
    public String REFERENCE_CYCLE_ERROR = "ReferenceCycle";
  }
}
