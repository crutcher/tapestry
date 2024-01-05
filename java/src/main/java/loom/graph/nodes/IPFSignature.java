package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.common.json.WithSchema;
import loom.polyhedral.IndexProjectionFunction;

@Value
@Jacksonized
@Builder
@WithSchema(
    """
                {
                    "type": "object",
                    "properties": {
                        "inputs": { "$ref": "#/definitions/IPFMap" },
                        "outputs": { "$ref": "#/definitions/IPFMap" }
                    },
                    "required": ["inputs", "outputs"],
                    "additionalProperties": false,
                    "definitions": {
                        "IPFMap": {
                            "type": "object",
                            "patternProperties": {
                              "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                                    "type": "array",
                                    "items": { "$ref": "#/definitions/IndexProjectionFunction" }
                                }
                            }
                        },
                        "IndexProjectionFunction": {
                            "type": "object",
                            "properties": {
                                "affineMap": { "$ref": "#/definitions/ZAffineMap" },
                                "shape": { "$ref": "#/definitions/ZVector" }
                            },
                            "required": ["affineMap", "shape"],
                            "additionalProperties": false
                        },
                        "ZAffineMap": {
                            "type": "object",
                            "properties": {
                                "A": { "$ref": "#/definitions/ZMatrix" },
                                "b": { "$ref": "#/definitions/ZVector" }
                            },
                            "required": ["A", "b"],
                            "additionalProperties": false
                        },
                        "ZVector": {
                            "type": "array",
                            "items": {
                              "type": "integer"
                            }
                        },
                        "ZMatrix": {
                            "description": "A matrix of integers; must be non-ragged",
                            "type": "array",
                            "items": {
                                "type": "array",
                                "items": {
                                    "type": "integer"
                                }
                            }
                        }
                      }
                    }
                }
                """)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IPFSignature implements HasToJsonString {
  @Singular @Nonnull Map<String, List<IndexProjectionFunction>> inputs;
  @Singular @Nonnull Map<String, List<IndexProjectionFunction>> outputs;
}
