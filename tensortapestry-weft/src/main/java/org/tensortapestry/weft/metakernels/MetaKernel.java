package org.tensortapestry.weft.metakernels;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorSelection;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorSelectionSupplier;

@Getter
@RequiredArgsConstructor
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
    public CallBuilder input(String key, List<TensorSelectionSupplier> items) {
      this.inputs.put(
          key,
          items.stream().map(TensorSelectionSupplier::getTensorSelection).toList()
        );
      return this;
    }

    @CanIgnoreReturnValue
    public CallBuilder input(String key, TensorSelectionSupplier... items) {
      return input(key, List.of(items));
    }

    @CanIgnoreReturnValue
    public CallBuilder input(String key, TensorSelectionSupplier item) {
      return input(key, List.of(item));
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

  @Nonnull
  private final String kernelName;

  public CallBuilder on(@Nonnull LoomGraph graph) {
    return new CallBuilder().graph(graph);
  }

  public abstract OperationNode apply(
    @Nonnull LoomGraph graph,
    @Nullable Map<String, List<TensorSelection>> inputs,
    @Nullable Map<String, Object> params
  );
}
