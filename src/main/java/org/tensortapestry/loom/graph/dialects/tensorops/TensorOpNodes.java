package org.tensortapestry.loom.graph.dialects.tensorops;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TensorOpNodes {

  // TODO: Switch to lookup by anchor when this is fixed:
  // See: https://github.com/networknt/json-schema-validator/pull/930

  public final String IPF_SIGNATURE_ANNOTATION_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd#/$defs/IPFSignature";
  public final String IPF_INDEX_ANNOTATION_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd#/$defs/IPFIndex";
}
