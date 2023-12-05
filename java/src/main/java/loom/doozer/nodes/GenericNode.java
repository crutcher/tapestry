package loom.doozer.nodes;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.doozer.DoozerGraph;

@Jacksonized
@SuperBuilder
public class GenericNode extends DoozerGraph.Node<GenericNode, GenericNode.Body> {
  @Data
  public static class Body {
    private Map<String, Object> fields;

    @JsonCreator
    public Body(Map<String, Object> fields) {
      this.fields = fields;
    }

    @JsonAnySetter
    public void setField(String name, Object value) {
      fields.put(name, value);
    }

    @JsonValue
    @JsonAnyGetter
    public Map<String, Object> getFields() {
      return fields;
    }
  }

  public static final class Meta extends NodeMeta<GenericNode, Body> {
    public static final String BODY_SCHEMA =
        """
                {
                  "type": "object",
                  "patternProperties": {
                    "^[a-zA-Z_][a-zA-Z0-9_]*$": {}
                  }
                }
                """;

    public Meta() {
      super(GenericNode.class, Body.class, BODY_SCHEMA);
    }
  }
  ;

  /** Exists to support {@code @Delegate} for {@code getBody()}. */
  @Delegate
  private Body delegateProvider() {
    return getBody();
  }
}
