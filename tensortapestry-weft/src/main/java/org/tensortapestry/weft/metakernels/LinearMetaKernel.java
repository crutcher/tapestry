package org.tensortapestry.weft.metakernels;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.*;

public class LinearMetaKernel extends DataTypeCheckingMetaKernel {

  private static final String KERNEL_NAME = "op:linear";

  public LinearMetaKernel(Set<String> dataTypes) {
    super(dataTypes);
  }

  @Override
  public OperationNode apply(
    @Nonnull LoomGraph graph,
    @Nullable Map<String, List<TensorSelection>> inputs,
    @Nullable Map<String, Object> params
  ) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
