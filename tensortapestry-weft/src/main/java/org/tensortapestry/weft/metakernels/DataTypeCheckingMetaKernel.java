package org.tensortapestry.weft.metakernels;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorSelection;

@Getter
public abstract class DataTypeCheckingMetaKernel extends MetaKernel {

  @Nonnull
  private final Set<String> dataTypes;

  public DataTypeCheckingMetaKernel(String kernelName, @Nonnull Set<String> dataTypes) {
    super(kernelName);
    this.dataTypes = Set.copyOf(dataTypes);
  }

  protected String uniformDtypeCheck(LoomGraph graph, List<TensorSelection> selections) {
    if (selections.isEmpty()) {
      return null;
    }
    var tensors = selections
      .stream()
      .map(TensorSelection::getTensorId)
      .map(id -> graph.assertNode(id, TensorNode.class))
      .toList();

    var seenTypes = tensors.stream().map(TensorNode::getDtype).collect(Collectors.toSet());
    if (seenTypes.size() != 1) {
      throw new IllegalArgumentException(
        "Expected all tensors to have the same dtype, found %s".formatted(seenTypes)
      );
    }
    var dtype = seenTypes.iterator().next();
    checkDataType(dtype);
    return dtype;
  }

  public List<TensorSelection> requiredList(String key, Map<String, List<TensorSelection>> inputs) {
    if (inputs == null || !inputs.containsKey(key)) {
      throw new IllegalArgumentException("Expected input key `%s`".formatted(key));
    }
    return inputs.get(key);
  }

  public TensorSelection requiredSingular(String key, Map<String, List<TensorSelection>> inputs) {
    var tensors = requiredList(key, inputs);
    if (tensors.size() != 1) {
      throw new IllegalArgumentException(
        "Expected exactly one tensor for input key `%s`".formatted(key)
      );
    }
    return tensors.getFirst();
  }

  public TensorSelection optionalSingular(String key, Map<String, List<TensorSelection>> inputs) {
    var tensors = inputs.get(key);
    if (tensors == null) {
      return null;
    }
    if (tensors.size() != 1) {
      throw new IllegalArgumentException(
        "Expected exactly one tensor for input key `%s`".formatted(key)
      );
    }
    return tensors.getFirst();
  }

  @CanIgnoreReturnValue
  protected String checkDataType(String dataType) {
    if (!dataTypes.contains(dataType)) {
      throw new IllegalArgumentException(
        "Unexpected dtype %s, expected one of %s".formatted(dataType, dataTypes)
      );
    }
    return dataType;
  }
}
