package org.tensortapestry.loom.graph;

import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.dialects.tensorops.TensorNode;

@SuppressWarnings("unused")
@UtilityClass
public class CommonSchemas {

  public final String ZTENSOR_SCHEMA =
    """
       {
         "name": "ZTensor",
         "$recursiveAnchor": true,
         "anyOf": [
           {
             "type": "integer"
           },
           {
             "type": "array",
             "items": {
               "$recursiveRef": "#"
             }
           }
         ]
      }
      """;

  public final String ZPOINT_SCHEMA =
    """
 {
   "name": "ZPoint",
   "type": "array",
   "items": {
     "type": "integer"
   }
}
""";

  public final String ZMATRIX_SCHEMA =
    """
      {
        "name": "ZMatrix",
        "type": "array",
        "items": {
          "type": "array",
          "items": {
            "type": "integer"
          }
        }
      }
      """;

  public final LoomTypeSchema NOTE_NODE_SCHEMA = LoomTypeSchema
    .builder()
    .jsonSchema(
      """
              {
                  "type": "object",
                  "properties": {
                    "text": {
                        "type": "string"
                    }
                  },
                  "required": ["text"],
                    "additionalProperties": false
              }
              """
    )
    .build();
  public final LoomTypeSchema TENSOR_NODE_SCHEMA = LoomTypeSchema
    .builder()
    .jsonSchema(
      """
              {
                  "type": "object",
                  "properties": {
                    "dtype": {
                        "type": "string"
                    },
                    "range": { "$ref": "#/definitions/ZRange" }
                  },
                  "required": ["dtype", "range"],
                    "additionalProperties": false,
                    "definitions": {
                      "ZRange": {
                          "type": "object",
                          "properties": {
                              "start": { "$ref": "#/definitions/ZPoint" },
                              "end": { "$ref": "#/definitions/ZPoint" }
                          },
                          "required": ["start", "end"],
                          "additionalProperties": false
                      },
                      "ZPoint": {
                          "type": "array",
                          "items": {
                            "type": "integer"
                          }
                      }
                  }
              }
              """
    )
    .build();

  public final LoomTypeSchema APPLICATION_NODE_SCHEMA = LoomTypeSchema
    .builder()
    .referenceSchema(
      "inputs",
      LoomTypeSchema.ReferenceSchema
        .builder()
        .path("$.inputs.*[*].tensorId")
        .type(TensorNode.TYPE)
        .build()
    )
    .referenceSchema(
      "outputs",
      LoomTypeSchema.ReferenceSchema
        .builder()
        .path("$.outputs.*[*].tensorId")
        .type(TensorNode.TYPE)
        .build()
    )
    .jsonSchema(
      """
              {
                  "type": "object",
                  "properties": {
                      "operationId": {
                          "type": "string",
                          "format": "uuid"
                      },
                      "inputs": { "$ref": "#/definitions/TensorSelectionMap" },
                      "outputs": { "$ref": "#/definitions/TensorSelectionMap" }
                  },
                  "required": ["operationId"],
                  "additionalProperties": false,
                  "definitions": {
                      "TensorSelectionMap": {
                          "type": "object",
                          "patternProperties": {
                              "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                                  "type": "array",
                                  "items": { "$ref": "#/definitions/TensorSelection" },
                                  "minItems": 1
                              }
                          },
                          "additionalProperties": false
                      },
                      "TensorSelection": {
                          "type": "object",
                          "properties": {
                              "tensorId": {
                                  "type": "string",
                                  "format": "uuid"
                              },
                              "range": { "$ref": "#/definitions/ZRange" }
                          },
                          "required": ["tensorId", "range"],
                          "additionalProperties": false
                      },
                      "ZRange": {
                          "type": "object",
                          "properties": {
                               "start": { "$ref": "#/definitions/ZPoint" },
                               "end": { "$ref": "#/definitions/ZPoint" }
                          },
                          "required": ["start", "end"]
                      },
                      "ZPoint": {
                          "type": "array",
                          "items": {
                              "type": "integer"
                          }
                      }
                  }
              }
              """
    )
    .build();

  public final LoomTypeSchema OPERATION_SIGNATURE_NODE_SCHEMA = LoomTypeSchema
    .builder()
    .referenceSchema(
      "inputs",
      LoomTypeSchema.ReferenceSchema
        .builder()
        .path("$.inputs.*[*].tensorId")
        .type(TensorNode.TYPE)
        .build()
    )
    .referenceSchema(
      "outputs",
      LoomTypeSchema.ReferenceSchema
        .builder()
        .path("$.outputs.*[*].tensorId")
        .type(TensorNode.TYPE)
        .build()
    )
    .jsonSchema(
      """
              {
                  "type": "object",
                  "properties": {
                      "kernel": {
                          "type": "string"
                      },
                      "params": {
                          "type": "object",
                          "patternProperties": {
                              "^[a-zA-Z_][a-zA-Z0-9_]*$": {}
                          },
                          "additionalProperties": false
                      },
                      "inputs": { "$ref": "#/definitions/TensorSelectionMap" },
                      "outputs": { "$ref": "#/definitions/TensorSelectionMap" }
                  },
                  "required": ["kernel"],
                  "additionalProperties": false,
                  "definitions": {
                      "TensorSelectionMap": {
                          "type": "object",
                          "patternProperties": {
                              "^[a-zA-Z_][a-zA-Z0-9_]*$": {
                                  "type": "array",
                                  "items": { "$ref": "#/definitions/TensorSelection" },
                                  "minItems": 1
                              }
                          },
                          "additionalProperties": false
                      },
                      "TensorSelection": {
                          "type": "object",
                          "properties": {
                              "tensorId": {
                                  "type": "string",
                                  "format": "uuid"
                              },
                              "range": { "$ref": "#/definitions/ZRange" }
                          },
                          "required": ["tensorId", "range"],
                          "additionalProperties": false
                      },
                      "ZRange": {
                          "type": "object",
                          "properties": {
                               "start": { "$ref": "#/definitions/ZPoint" },
                               "end": { "$ref": "#/definitions/ZPoint" }
                          },
                          "required": ["start", "end"]
                      },
                      "ZPoint": {
                          "type": "array",
                          "items": {
                              "type": "integer"
                          }
                      }
                  }
              }
              """
    )
    .build();
}
