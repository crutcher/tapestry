package org.tensortapestry.weft.metakernels;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Data;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorSelection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MetaKernel {

  @Data
  public class CallBuilder {

    private LoomGraph graph;

    @Nonnull
    private Map<String, List<TensorSelection>> inputs = new HashMap<>();

    @Nonnull
    private Map<String, Object> params = new HashMap<>();

    @CanIgnoreReturnValue
    public CallBuilder graph(LoomGraph graph) {
      this.graph = graph;
      return this;
    }

    @CanIgnoreReturnValue
    public CallBuilder input(String key, List<TensorSelection> items) {
      this.inputs.put(key, items);
      return this;
    }

    @CanIgnoreReturnValue
    public CallBuilder param(String key, Object value) {
      this.params.put(key, value);
      return this;
    }

    public OperationNode apply() {
      return MetaKernel.this.apply(graph, inputs, params);
    }
  }

  public CallBuilder on(@Nonnull LoomGraph graph) {
    return new CallBuilder().graph(graph);
  }

  public abstract OperationNode apply(
    @Nonnull LoomGraph graph,
    @Nullable Map<String, List<TensorSelection>> inputs,
    @Nullable Map<String, Object> params
  );
}
