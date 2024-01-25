package org.tensortapestry.loom.graph.dialects.tensorops;

import static org.tensortapestry.loom.graph.LoomConstants.LOOM_CORE_NODE_TYPE;

import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.LoomConstants;

@UtilityClass
public class TensorOpNodes {

  public final String TENSOR_NODE_TYPE = LOOM_CORE_NODE_TYPE.apply("Tensor");
  public final String APPLICATION_NODE_TYPE = LOOM_CORE_NODE_TYPE.apply("Application");
  public final String OPERATION_SIGNATURE_NODE_TYPE = LOOM_CORE_NODE_TYPE.apply(
    "OperationSignature"
  );

  public final String IPF_SIGNATURE_ANNOTATION_TYPE = LoomConstants.LOOM_CORE_ANNOTATION_TYPE.apply(
    "IPFSignature"
  );
  public final String IPF_INDEX_ANNOTATION_TYPE = LoomConstants.LOOM_CORE_ANNOTATION_TYPE.apply(
    "IPFIndex"
  );
}
