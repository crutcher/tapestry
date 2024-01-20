package loom.graph.constraints;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import loom.common.json.JsonPathUtils;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.graph.LoomTypeSchema;
import loom.validation.ValidationIssueCollector;

@Data
@Builder
public class TypeSchemaConstraint implements LoomEnvironment.Constraint {
  @Singular private final Map<String, LoomTypeSchema> nodeTypeSchemas;
  @Singular private final Map<String, LoomTypeSchema> annotationTypeSchemas;

  @Override
  public void validateConstraint(
      LoomEnvironment env, LoomGraph graph, ValidationIssueCollector collector) {
    graph
        .nodeScan()
        .asStream()
        .forEach(
            node -> {
              var nodeSchema = nodeTypeSchemas.get(node.getType());
              if (nodeSchema != null) {
                nodeSchema.validateValue(
                    env,
                    graph,
                    JsonPathUtils.concatJsonPath(node.getJsonPath(), "body"),
                    node.getBody(),
                    collector);
              }

              node.getAnnotations()
                  .forEach(
                      (key, value) -> {
                        var annotationSchema = annotationTypeSchemas.get(key);
                        if (annotationSchema != null) {
                          annotationSchema.validateValue(
                              env,
                              graph,
                              JsonPathUtils.concatJsonPath(node.getJsonPath(), "annotations", key),
                              value,
                              collector);
                        }
                      });
            });
  }
}
