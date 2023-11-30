package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import loom.common.serialization.JsonUtil;
import loom.graph.Constants;
import loom.graph.LoomGraph;
import loom.graph.LoomGraphEnv;
import loom.graph.validation.ValidationIssue;
import loom.graph.validation.ValidationIssueCollector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OperationNodeTypeBindings extends NodeTypeBindings {
  public static final String OPERATION_TYPE = Constants.LOOM_NS + "#types/operation";

  public static final String IO_KEY_REGEX = "[a-zA-Z_][a-zA-Z0-9_]*";

  public static final String OPERATION_FIELD_SCHEMA =
      """
      {
          "type": "object",
          "properties": {
              "inputs": {
                  "type": "object",
                  "patternProperties": {
                      "%1$s": {
                          "type": "array",
                          "items": {
                              "type": "string",
                              "format": "uuid"
                          }
                      }
                  },
                  "additionalProperties": false
              },
              "outputs": {
                  "type": "object",
                  "patternProperties": {
                      "%1$s": {
                          "type": "array",
                          "items": {
                              "type": "string",
                              "format": "uuid"
                          }
                      }
                  },
                  "additionalProperties": false
              }
          },
          "additionalProperties": false
      }
      """
          .formatted(IO_KEY_REGEX);
  public static final String MISSING_TARGET = "MissingTarget";

  @Data
  @Jacksonized
  @Builder
  public static class OperationFields {
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, List<UUID>> inputs = new HashMap<>();

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, List<UUID>> outputs = new HashMap<>();
  }

  public static OperationFields parseFields(LoomGraph.NodeDom node) {
    return JsonUtil.convertValue(node.getFields(), OperationFields.class);
  }

  public OperationNodeTypeBindings() {
    super(OPERATION_TYPE, OPERATION_FIELD_SCHEMA);
  }

  @Override
  public void checkNodeSemantics(
      LoomGraphEnv env, LoomGraph.NodeDom node, ValidationIssueCollector issueCollector) {
    var graph = node.getGraph();

    for (var ioMap : new String[] {"inputs", "outputs"}) {
      if (!node.hasField(ioMap)) {
        continue;
      }

      @SuppressWarnings("unchecked")
      Map<String, List<String>> inputs = node.getFieldAsType(ioMap, Map.class);
      for (var entry : inputs.entrySet()) {
        var ioKey = entry.getKey();
        var targets = entry.getValue();

        for (var idx = 0; idx < targets.size(); idx++) {
          var target = targets.get(idx);
          if (!graph.hasNode(target)) {
            issueCollector.add(
                ValidationIssue.builder()
                    .type(Constants.TYPE_VALIDATION)
                    .param("type", OPERATION_TYPE)
                    .param("error", MISSING_TARGET)
                    .summary("Target node (%s) does not exist".formatted(target))
                    .context(
                        ValidationIssue.Context.builder()
                            .name("Target")
                            .jsonpath("$.fields.%s.%s[%d]".formatted(ioMap, ioKey, idx))
                            .dataFromTree(target)
                            .build()));
          }
        }
      }
    }
  }
}
