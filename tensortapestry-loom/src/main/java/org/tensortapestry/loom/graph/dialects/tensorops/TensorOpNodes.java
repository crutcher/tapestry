package org.tensortapestry.loom.graph.dialects.tensorops;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TensorOpNodes {

  // TODO: Switch to lookup by anchor when this is fixed:
  // See: https://github.com/networknt/json-schema-validator/pull/930

  public final String IO_SEQUENCE_POINT_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd#/annotations/IOSequencePoint";

  public final String IPF_SIGNATURE_ANNOTATION_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd#/annotations/IPFSignature";
  public final String IPF_INDEX_ANNOTATION_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/annotation_types.jsd#/annotations/IPFIndex";
}
