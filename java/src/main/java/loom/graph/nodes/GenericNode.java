package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomGraph;

/**
 * The GenericNode class represents a generic node in a graph. It extends the LoomGraph.Node class,
 * with a GenericNode.Body class representing the body of the node.
 */
@Jacksonized
@SuperBuilder
@Getter
@Setter
public final class GenericNode extends LoomGraph.Node<GenericNode, GenericNode.Body> {
  @Delegate @Nonnull private Body body;

  /** The Body class represents the body of a GenericNode. It contains a map of fields. */
  @Data
  @Builder
  public static class Body {
    @Singular private Map<String, Object> fields;

    /**
     * This method is a constructor for the Body class. It initializes the fields map with the given
     * map of fields.
     *
     * @param fields a map representing the fields of the Body object
     */
    @JsonCreator
    public Body(Map<String, Object> fields) {
      setFields(fields);
    }

    /**
     * Sets the value of a field in the GenericNode.
     *
     * @param name the name of the field
     * @param value the value of the field
     */
    @JsonAnySetter
    public void setField(String name, Object value) {
      fields.put(name, value);
    }

    /**
     * Retrieves the value of a field from the GenericNode's body.
     *
     * @param name the name of the field
     * @return the value of the field
     */
    public Object getField(String name) {
      return fields.get(name);
    }

    /**
     * Retrieves the fields of the Body object.
     *
     * @return a map representing the fields of the Body object
     */
    @JsonValue
    @JsonAnyGetter
    public Map<String, Object> getFields() {
      return fields;
    }

    /**
     * Sets the fields of the Body object with the given map of fields.
     *
     * @param fields a map representing the fields of the Body object
     */
    public void setFields(Map<String, Object> fields) {
      this.fields = new HashMap<>(fields);
    }
  }

  /**
   * The Prototype class represents a node prototype for the GenericNode class. It extends the
   * LoomGraph.NodePrototype class and provides a schema for the body of the node.
   */
  public static final class Prototype extends LoomGraph.NodePrototype<GenericNode, Body> {
    public static final String BODY_SCHEMA =
        """
                {
                  "type": "object",
                  "patternProperties": {
                    "^[a-zA-Z_][a-zA-Z0-9_]*$": {}
                  },
                  "additionalProperties": false
                }
                """;

    public Prototype() {
      super(GenericNode.class, Body.class, BODY_SCHEMA);
    }
  }

  /**
   * The Prototype class represents a node prototype for the GenericNode class. It extends the
   * LoomGraph.NodePrototype class and provides a schema for the body of the node.
   */
  public static final Prototype PROTOTYPE = new Prototype();
}
