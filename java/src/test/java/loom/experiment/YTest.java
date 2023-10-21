package loom.experiment;

import java.util.UUID;
import lombok.ToString;
import loom.common.serialization.JsonUtil;
import loom.testing.CommonAssertions;
import org.graalvm.polyglot.Context;
import org.junit.Test;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.tomlj.Toml;

@SuppressWarnings("unused")
public class YTest implements CommonAssertions {
  @Test
  public void testYAJ() {
    var json =
        """
                {
                  "nodes": [
                    {
                      "uuid": "00000000-0000-0000-0000-000000000001",
                      "type": "{http://org.loom}tensor",
                      "fields": {
                        "dtype": "float32"
                      }
                    },
                    {
                      "uuid": "00000000-0000-0000-0000-000000000002",
                      "type": "{http://org.loom}tensor",
                      "fields": {
                        "dtype": "float32",
                        "shape": [30, 2]
                      }
                    },
                    {
                      "uuid": "00000000-0000-0000-0000-000000000003",
                      "type": "{http://org.loom}operation",
                      "fields": {
                        "op": "org.loom,2023:concat",
                        "inputs": {
                          "source": [
                            {
                              "ref": "00000000-0000-0000-0000-000000000001"
                            },
                            {
                              "ref": "00000000-0000-0000-0000-000000000002"
                            }
                          ]
                        },
                        "outputs": {
                          "out": [
                            {
                              "ref": "00000000-0000-0000-0000-000000000004"
                            }
                          ]
                        }
                      }
                    },
                    {
                      "uuid": "00000000-0000-0000-0000-000000000004",
                      "type": "org.loom,2023:tensor",
                      "fields": {
                        "dtype": "float32",
                        "shape": [60, 2]
                      }
                    }
                  ]
                }
                """;

    var data = JsonUtil.parseToMap(json);
    System.out.println(JsonUtil.toPrettyJson(data));

    var jsCode =
        """
                function validateTensor(tensor) {
                    console.log("validateTensor: " + tensor);

                    if (!("shape" in tensor.fields)) {
                        console.error("shape not in tensor.fields");
                    }
                }

                function validateOperation(operation) {
                    console.log("validateOperation: " + operation);
                }

                TYPE_HANDLERS = {
                  "{http://org.loom}tensor": validateTensor,
                  "{http://org.loom}operation": validateOperation,
                };

                function validate(graph) {
                    graph.nodes.forEach(function (node) {
                      if (node.type in TYPE_HANDLERS) {
                        TYPE_HANDLERS[node.type](node);
                      } else {
                        console.log("Unknown node type: " + node.type);
                      }
                    });
                }
                """;

    try (var context = Context.create()) {
      context.eval("js", jsCode);
      var jsData = context.getBindings("js").getMember("JSON").getMember("parse").execute(json);
      var validate = context.getBindings("js").getMember("validate");
      validate.execute(jsData);
    }
  }

  @Test
  public void testPluginInsertion() {
    var baseCode =
        """
                PLUGINS = {};

                function registerPlugin(plugin) {
                    PLUGINS[plugin.id] = plugin;
                }

                function delegate(pluginId, name, ...args) {
                    if (pluginId in PLUGINS) {
                        var plugin = PLUGINS[pluginId];
                        if (name in plugin) {
                            return plugin[name](...args);
                        } else {
                            console.error("Plugin " + pluginId + " does not have method " + name);
                        }
                    } else {
                        console.error("Plugin " + pluginId + " not registered");
                    }
                }
                """;

    try (var context = Context.create()) {
      context.eval("js", baseCode);

      var pluginCode =
          """
                    registerPlugin({
                        id: "abc",
                        foo: function (x) {
                            return "foo: " + x;
                        }
                    });
                    """;

      context.eval("js", pluginCode);

      assertThat(
              context
                  .getBindings("js")
                  .getMember("delegate")
                  .execute("abc", "foo", "bar")
                  .toString())
          .isEqualTo("foo: bar");
    }
  }

  @Test
  public void testYamlStuff() {
    var settings = LoadSettings.builder().build();
    Load load = new Load(settings);

    var yaml =
        """
                expression_graph:
                  nodes:
                    - type: org.loom,2023:tensor
                      uuid: 00000000-0000-0000-0000-000000000001
                      fields:
                        dtype: float32
                        shape: [30, 2]
                    - type: org.loom,2023:tensor
                      uuid: 00000000-0000-0000-0000-000000000002
                      fields:
                        dtype: float32
                        shape: [30, 2]
                    - type: org.loom,2023:operation
                      uuid: 00000000-0000-0000-0000-000000000003
                      fields:
                        op: org.loom,2023:concat
                        inputs:
                          source:
                            - ref: 00000000-0000-0000-0000-000000000001
                            - ref: 00000000-0000-0000-0000-000000000002
                        outputs:
                          out:
                            - ref: 00000000-0000-0000-0000-000000000004
                    - type: org.loom,2023:tensor
                      uuid: 00000000-0000-0000-0000-000000000004
                      fields:
                        dtype: float32
                        shape: [60, 2]
                """;

    var node = load.loadFromString(yaml);

    System.out.println(JsonUtil.toPrettyJson(node));
  }

  @ToString
  public static class Tensor {
    public UUID uuid;
    public String dtype;
    public int[] shape;
  }

  @Test
  public void testTOML() {

    var data =
        Toml.parse(
            """
                graph_id = "00000000-0000-0000-0000-0000000000AA"

                [[nodes]]
                type = "org.loom,2023:tensor"
                uuid = "00000000-0000-0000-0000-000000000001"

                [nodes.fields]
                dtype = "float32"
                shape = [30, 2]


                [[nodes]]
                type = "org.loom,2023:tensor"
                uuid = "00000000-0000-0000-0000-000000000002"
                [nodes.fields]
                dtype = "float32"
                shape = [30, 2]

                [[nodes]]
                type = "org.loom,2023:operation"
                uuid = "00000000-0000-0000-0000-000000000003"
                [nodes.fields]
                op = "org.loom,2023:concat"
                [[nodes.fields.inputs]]
                source = ["00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002"]
                [[nodes.fields.outputs]]
                out = ["00000000-0000-0000-0000-000000000004"]

                [[nodes]]
                type = "org.loom,2023:tensor"
                uuid = "00000000-0000-0000-0000-000000000004"
                [nodes.fields]
                dtype = "float32"
                shape = [60, 2]
                """);

    System.out.println(data.toJson());

    System.out.println(JsonUtil.toPrettyJson(data.toMap()));
  }
}
