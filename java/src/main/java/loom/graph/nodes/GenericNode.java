package loom.graph.nodes;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomGraph;

import java.util.Map;

@Jacksonized
@SuperBuilder
public final class GenericNode extends LoomGraph.Node<GenericNode, GenericNode.Body> {
  @Data
  @Builder
  public static class Body {
    @Singular private Map<String, Object> fields;

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

  @Override
  public Class<Body> getBodyClass() {
    return Body.class;
  }

  public static final class Meta extends LoomGraph.NodeMeta<GenericNode, Body> {
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

  public static final Meta META = new Meta();

  /** Exists to support {@code @Delegate} for {@code getBody()}. */
  @SuppressWarnings("unused")
  @Delegate
  private Body delegateProvider() {
    return getBody();
  }
}
