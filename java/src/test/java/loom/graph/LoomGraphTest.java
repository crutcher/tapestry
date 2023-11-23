package loom.graph;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import loom.common.serialization.JsonUtil;
import loom.graph.nodes.OperationNodeTypeOps;
import loom.graph.nodes.TensorNodeTypeOps;
import loom.testing.BaseTestClass;
import loom.validation.LoomValidationError;
import loom.validation.ValidationIssue;
import org.junit.Test;

public class LoomGraphTest extends BaseTestClass {
  @Test
  public void testNodeJPath() {
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
                         "b": [2, 3],
                         "a": 1
                       }
                     }
                  ]
                }
                """
            .formatted(nodeId);

    var env = LoomGraphEnv.createDefault();
    var graph = env.parse(source);

    LoomGraph.NodeDom nodeDom = graph.assertNode(nodeId);
    assertThat(nodeDom.jpath()).isEqualTo("$.nodes[?(@.id=='%s')]".formatted(nodeId));

    List<Map<String, Object>> nodeDoc = JsonPath.parse(graph.toJsonString()).read(nodeDom.jpath());
    assertThat(nodeDoc.get(0).get("id")).isEqualTo(nodeId);
  }

  @Test
  public void testParse() {
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
                     "b": [2, 3],
                     "a": 1
                   }
                 }
              ]
            }
            """
            .formatted(nodeId);

    var env = LoomGraphEnv.createDefault();
    assertJsonEquals(env.wrap(JsonUtil.fromJson(source, LoomDoc.class)).getDoc(), source);
    assertJsonEquals(env.parse(source).getDoc(), source);
    assertJsonEquals(env.parse(JsonUtil.readTree(source)).getDoc(), source);
  }

  @Test
  public void testDup() {
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

    var env = LoomGraphEnv.createDefault();

    var graph = env.wrap(doc);

    var dup = graph.deepCopy();

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
    var env = LoomGraphEnv.createDefault();

    var graph = env.wrap(doc);
    assertJsonEquals(graph.getDoc(), source);

    assertJsonEquals(graph.getDoc(), graph.toJsonString());
  }

  @Test
  public void testValidatePass() {
    var tensorA = "00000000-0000-4000-8000-000000000001";
    var operationA = "00000000-0000-4000-8000-100000000001";

    String source =
        """
                {
                  "nodes": [
                     {
                       "id": "%1$s",
                       "type": "%2$s",
                       "fields": {
                         "shape": [2, 3, 1],
                         "dtype": "int32"
                       }
                     },
                     {
                        "id": "%3$s",
                         "type": "%4$s",
                         "fields": {
                             "inputs": {
                                 "sources": [ "%1$s" ]
                             },
                             "outputs": {
                                 "sources": [ "%1$s" ]
                             }
                         }
                     }
                  ]
                }
                """
            .formatted(
                tensorA,
                TensorNodeTypeOps.TENSOR_TYPE,
                operationA,
                OperationNodeTypeOps.OPERATION_TYPE);

    var env = LoomGraphEnv.createDefault();
    env.parse(source).validate();

    LoomGraph graph = env.wrap(JsonUtil.fromJson(source, LoomDoc.class));
    assertJsonEquals(graph.getDoc(), source);

    graph.validate();
  }

  @Test
  public void testValidateFail() {
    var nodeId = "00000000-0000-4000-8000-000000000001";

    String source =
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
            .formatted(nodeId, TensorNodeTypeOps.TENSOR_TYPE);

    var env = LoomGraphEnv.createDefault();
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
        .extracting(ValidationIssue::formattedType)
        .containsExactlyInAnyOrder(
            "JsdError{error=MinimumError}",
            "JsdError{error=FalseSchemaError}",
            "TypeValidation{field=dtype, type=%s}".formatted(TensorNodeTypeOps.TENSOR_TYPE));
  }
}
