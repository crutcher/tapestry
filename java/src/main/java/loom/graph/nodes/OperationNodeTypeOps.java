package loom.graph.nodes;

import java.util.List;
import java.util.Map;
import loom.graph.LoomGraph;
import loom.graph.LoomGraphEnv;
import loom.validation.Constants;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

public class OperationNodeTypeOps extends LoomGraphEnv.LoomNodeTypeOps {
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

  public OperationNodeTypeOps() {
    super(OPERATION_TYPE, OPERATION_FIELD_SCHEMA);
  }

  @Override
  public void checkNode(LoomGraphEnv env, LoomGraph.NodeDom node) {
    var graph = node.getGraph();
    var issues = new ValidationIssueCollector();

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
            issues.add(
                ValidationIssue.builder()
                    .type(Constants.TYPE_VALIDATION)
                    .param("type", OPERATION_TYPE)
                    .param("error", MISSING_TARGET)
                    .summary("Target node (%s) does not exist".formatted(target))
                    .context(
                        ValidationIssue.Context.builder()
                            .name("Target")
                            .jsonpath("$.fields.%s.%s[%d]".formatted(ioMap, ioKey, idx))
                            .dataToJson(target)
                            .build())
                    .build());
          }
        }
      }
    }

    issues.check();
  }
}
