package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import loom.common.HasToJsonString;
import loom.common.IdUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TTag.class),
      @JsonSubTypes.Type(value = TOperator.class),
      @JsonSubTypes.Type(value = TSequencePoint.class),
    })
public abstract class TNode implements HasToJsonString {
  @Nullable @JsonIgnore TGraph graph = null;

  @Nonnull
  @JsonProperty(required = true)
  public final UUID id;

  TNode(@Nullable UUID id) {
    this.id = IdUtils.coerceUUID(id);
  }

  TNode() {
    this(null);
  }

  public final boolean hasGraph() {
    return graph != null;
  }

  public final TGraph assertGraph() {
    if (graph == null) {
      throw new IllegalStateException("Node is not part of a graph");
    }
    return graph;
  }

  public void validate() {
    assertGraph();
  }

  public abstract TNode copy();
}
