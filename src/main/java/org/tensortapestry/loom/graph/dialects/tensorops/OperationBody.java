package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.graph.LoomNode;
import org.tensortapestry.loom.graph.dialects.common.JsdType;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "kernel", "params", "inputs", "outputs" })
@JsdType(TensorOpNodes.OPERATION_NODE_TYPE)
public class OperationBody implements HasToJsonString {

  @Nonnull
  String kernel;

  @Singular
  Map<String, Object> params;

  @Singular
  @Nonnull
  Map<String, List<TensorSelection>> inputs;

  @Singular
  @Nonnull
  Map<String, List<TensorSelection>> outputs;

  public static List<LoomNode> getShards(LoomNode opNode) {
    var id = opNode.getId();
    return opNode
      .assertGraph()
      .byType(TensorOpNodes.APPLICATION_NODE_TYPE)
      .stream()
      .filter(n -> n.viewBodyAs(ApplicationBody.class).getOperationId().equals(id))
      .toList();
  }
}
