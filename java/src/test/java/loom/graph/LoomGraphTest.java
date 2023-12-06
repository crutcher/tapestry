package loom.graph;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import loom.common.serialization.JsonUtil;
import loom.graph.nodes.TensorNodeTypeBindings;
import loom.testing.BaseTestClass;
import loom.validation.LoomValidationError;
import loom.validation.ValidationIssue;
import org.junit.Test;

public class LoomGraphTest extends BaseTestClass {
  @Test
  public void testNodeJPath() {
    var nodeId = "00000000-0000-4000-8000-000000000001";

    final String source =
        """
                {
                  "nodes": [
                     {
                       "id": "%s",
                       "type": "test",
                       "label": "Foo",
                       "fields": {
                         "b": [2, 3],
                         "a": 1
                       }
                     }
                  ]
                }
                """
            .formatted(nodeId);

    var env = CommonLoomGraphEnvironments.createDefault();
    var graph = env.parse(source);

    LoomGraph.NodeDom nodeDom = graph.assertNode(nodeId);
    assertThat(nodeDom.jpath()).isEqualTo("$.nodes[?(@.id=='%s')]".formatted(nodeId));

    List<Map<String, Object>> nodeDoc = JsonPath.parse(graph.toJsonString()).read(nodeDom.jpath());
    assertThat(nodeDoc.get(0).get("id")).isEqualTo(nodeId);
  }

  @Test
  public void testDup() {
    final String source =
        """
        {
          "nodes": [
             {
               "id": "%1$s",
               "type": "%2$s",
               "label": "Foo",
               "fields": {
                 "shape": [1, 3],
                 "dtype": "int32"
               }
             }
          ]
        }
        """
            .formatted("00000000-0000-4000-8000-000000000001", TensorNodeTypeBindings.TENSOR_TYPE);

    var env = CommonLoomGraphEnvironments.createDefault();
    var graph = env.parse(source);

    var dup = graph.deepCopy();

    assertThat(dup.getDoc()).isNotSameAs(graph.getDoc());
    assertJsonEquals(dup.getDoc(), source);
  }

  @Test
  public void testToJson() {
    var nodeId = "00000000-0000-4000-8000-000000000001";

    String source =
        """
        {
          "nodes": [
             {
               "id": "%s",
               "type": "test",
               "label": "Foo",
               "fields": {
                 "a": 1,
                 "b": [2, 3]
               }
             }
          ]
        }
        """
            .formatted(nodeId);

    var doc = JsonUtil.fromJson(source, LoomDoc.class);
    var env = CommonLoomGraphEnvironments.createDefault();

    var graph = env.wrap(doc);
    assertJsonEquals(graph.getDoc(), source);

    assertJsonEquals(graph.getDoc(), graph.toJsonString());
  }

  @Test
  public void testValidatePass() {
    var tensorA = "00000000-0000-4000-8000-000000000001";
    var operationA = "00000000-0000-4000-8000-0000000000A1";
    var operationB = "00000000-0000-4000-8000-0000000000A2";

    String source =
        """
                {
                  "nodes": [
                     {
                        "id": "%1$s",
                         "type": "https://loom-project.org/schema/v1/loom#types/operation",
                         "label": "source",
                         "fields": {
                             "outputs": {
                                 "data": [ "%2$s" ]
                             }
                         }
                     },
                     {
                       "id": "%2$s",
                       "type": "https://loom-project.org/schema/v1/loom#types/tensor",
                       "fields": {
                         "shape": [2, 3, 1],
                         "dtype": "int32"
                       }
                     },
                     {
                       "id": "%3$s",
                        "type": "https://loom-project.org/schema/v1/loom#types/operation",
                        "label": "sink",
                        "fields": {
                            "inputs": {
                                "sources": [ "%1$s" ]
                            }
                        }
                     }
                  ]
                }
                """
            .formatted(operationA, tensorA, operationB);

    var env = CommonLoomGraphEnvironments.createDefault();
    env.parse(source).validate();

    LoomGraph graph = env.wrap(JsonUtil.fromJson(source, LoomDoc.class));
    assertJsonEquals(graph.getDoc(), source);

    graph.validate();
  }

  @Test
  public void testValidateFail() {
    final String source =
        """
            {
              "nodes": [
                 {
                   "id": "%s",
                   "type": "%s",
                   "fields": {
                     "shape": [2, 3, -1],
                     "dtype": "int33",
                     "foo": "bar"
                   }
                 }
              ]
            }
            """
            .formatted("00000000-0000-4000-8000-000000000001", TensorNodeTypeBindings.TENSOR_TYPE);

    var env = CommonLoomGraphEnvironments.createDefault();
    var graph = env.parse(source);

    List<ValidationIssue> issues;
    try {
      graph.validate();
      fail("Expected validation error");
      return;
    } catch (LoomValidationError e) {
      issues = e.getIssues();
    }

    assertThat(issues)
        .extracting(ValidationIssue::typeDescription)
        .containsExactlyInAnyOrder(
            "JsdError{error=MinimumError}", "JsdError{error=FalseSchemaError}");
  }
}
