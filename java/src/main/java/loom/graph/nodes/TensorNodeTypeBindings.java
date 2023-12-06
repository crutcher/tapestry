package loom.graph.nodes;

import static loom.graph.Constants.TYPE_VALIDATION;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.JsonPathUtils;
import loom.common.serialization.JsonUtil;
import loom.graph.Constants;
import loom.graph.LoomGraph;
import loom.graph.LoomGraphEnv;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

@Builder
@EqualsAndHashCode(callSuper = false)
@Data
public class TensorNodeTypeBindings extends NodeTypeBindings {

  public static final String TENSOR_TYPE = Constants.LOOM_NS + "#types/tensor";

  public static final String TENSOR_FIELD_SCHEMA =
      """
            {
                "type": "object",
                "properties": {
                    "shape": {
                        "type": "array",
                        "items": {
                            "type": "integer",
                            "minimum": 1
                        }
                    },
                    "dtype": {
                        "type": "string"
                    }
                },
                "required": ["shape", "dtype"],
                "additionalProperties": false
           }
          """;

  @Data
  @Jacksonized
  @Builder
  public static class TensorFields {
    private int[] shape;
    private String dtype;
  }

  @Singular private final Set<String> datatypes = new HashSet<>();

  public TensorNodeTypeBindings() {
    super(TENSOR_TYPE, TENSOR_FIELD_SCHEMA);
  }

  public static TensorFields parseFields(LoomGraph.NodeDom node) {
    return JsonUtil.convertValue(node.getFields(), TensorNodeTypeBindings.TensorFields.class);
  }

  /**
   * Add a datatype to the set of valid datatypes.
   *
   * @param datatype the datatype to add.
   */
  public void addDatatype(String datatype) {
    datatypes.add(datatype);
  }

  @Override
  public void checkNodeSemantics(
      LoomGraphEnv env, LoomGraph.NodeDom node, ValidationIssueCollector issueCollector) {
    if (!datatypes.isEmpty()) {
      var dtype = node.getFieldAsType("dtype", String.class);
      if (!datatypes.contains(dtype)) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(TYPE_VALIDATION)
                .param("type", TENSOR_TYPE)
                .param("field", "dtype")
                .summary("Invalid tensor dtype (%s)".formatted(dtype))
                .message("Tensor dtype (" + dtype + ") must be one of: " + datatypes)
                .context(
                    ValidationIssue.Context.builder()
                        .name("DType")
                        .jsonpath(JsonPathUtils.concatJsonPath(node.jpath(), ".fields.dtype"))
                        .dataFromTree(dtype)
                        .build()));
      }
    }

    final var nodeId = node.getId();
    LoomGraph.NodeDom tensorSource = null;

    for (var opNode : node.getGraph().nodes(OperationNodeTypeBindings.OPERATION_TYPE)) {
      var fields = OperationNodeTypeBindings.parseFields(opNode);
      for (var outputIds : fields.getOutputs().values()) {
        if (outputIds.contains(nodeId)) {
          if (tensorSource == null) {
            tensorSource = opNode;
          } else {
            issueCollector.add(
                ValidationIssue.builder()
                    .type(TYPE_VALIDATION)
                    .param("type", TENSOR_TYPE)
                    .param("error", "MultipleProducers")
                    .summary(
                        "Tensor %s is produced by multiple operations: %s and %s"
                            .formatted(node.getId(), tensorSource, opNode.getId()))
                    .context(
                        ValidationIssue.Context.builder()
                            .name("Tensor")
                            .jsonpath(node.jpath())
                            .dataFromTree(node.getDoc())
                            .build())
                    .context(
                        ValidationIssue.Context.builder()
                            .name("Source 1")
                            .jsonpath(tensorSource.jpath())
                            .dataFromTree(opNode.getDoc())
                            .build())
                    .context(
                        ValidationIssue.Context.builder()
                            .name("Source 2")
                            .jsonpath(opNode.jpath())
                            .dataFromTree(opNode.getDoc())
                            .build()));
          }
        }
      }
    }
  }
}
