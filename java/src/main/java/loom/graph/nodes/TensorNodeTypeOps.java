package loom.graph.nodes;

import static loom.validation.Constants.TYPE_VALIDATION;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.JsonPathUtils;
import loom.common.serialization.JsonUtil;
import loom.graph.LoomGraph;
import loom.graph.LoomGraphEnv;
import loom.validation.Constants;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

public class TensorNodeTypeOps extends LoomGraphEnv.LoomNodeTypeOps {

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

  @Getter public Set<String> datatypes = new HashSet<>();

  public TensorNodeTypeOps() {
    super(TENSOR_TYPE, TENSOR_FIELD_SCHEMA);
  }

  public static TensorFields parseFields(LoomGraph.NodeDom node) {
    return JsonUtil.convertValue(node.getFields(), TensorNodeTypeOps.TensorFields.class);
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
  public void checkNodeSemantics(LoomGraphEnv env, LoomGraph.NodeDom node) {
    var issues = new ValidationIssueCollector();
    if (!datatypes.isEmpty()) {
      var dtype = node.getFieldAsType("dtype", String.class);
      if (!datatypes.contains(dtype)) {
        issues.add(
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
                        .build())
                .build());
      }
    }

    issues.check();
  }
}
