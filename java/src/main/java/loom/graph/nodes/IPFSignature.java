package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
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
"""
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IPFSignature implements HasToJsonString {

  public static final String ANNOTATION_TYPE = "IPFSignature";

  @SuppressWarnings("unused")
  public static class IPFSignatureBuilder {
    {
      this.inputs = new HashMap<>();
      this.outputs = new HashMap<>();
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder inputs(@Nonnull Map<String, List<IndexProjectionFunction>> ipfs) {
      this.inputs = ipfs;
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder input(
      @Nonnull String name,
      @Nonnull List<IndexProjectionFunction> ipfs
    ) {
      this.inputs.put(name, new ArrayList<>(ipfs));
      return this;
    }

    public IPFSignatureBuilder input(@Nonnull String name, @Nonnull IndexProjectionFunction ipf) {
      this.inputs.computeIfAbsent(name, k -> new ArrayList<>()).add(ipf);
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder outputs(@Nonnull Map<String, List<IndexProjectionFunction>> ipfs) {
      this.outputs = ipfs;
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder output(
      @Nonnull String name,
      @Nonnull List<IndexProjectionFunction> ipfs
    ) {
      this.outputs.put(name, new ArrayList<>(ipfs));
      return this;
    }

    public IPFSignatureBuilder output(@Nonnull String name, @Nonnull IndexProjectionFunction ipf) {
      this.outputs.computeIfAbsent(name, k -> new ArrayList<>()).add(ipf);
      return this;
    }
  }

  @Nonnull
  Map<String, List<IndexProjectionFunction>> inputs;

  @Nonnull
  Map<String, List<IndexProjectionFunction>> outputs;
}
