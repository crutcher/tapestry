package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@JsonTypeName("ResultOf")
@TTagBase.SourceType(TTensor.class)
@TEdgeBase.TargetType(TOperatorBase.class)
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {@TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#A7E1D5")})
public final class TResultEdge extends TEdgeBase<TTensor, TOperatorBase> {
  @Nonnull @Getter private final String name;

  @JsonCreator
  public TResultEdge(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
      @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId,
      @Nonnull @JsonProperty(value = "name", required = true) String name) {
    super(id, sourceId, targetId);
    this.name = name;
  }

  public TResultEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String name) {
    this(null, sourceId, targetId, name);
  }

  public TResultEdge(@Nonnull TResultEdge source) {
    this(source.id, source.sourceId, source.targetId, source.name);
  }

  @Override
  public TResultEdge copy() {
    return new TResultEdge(this);
  }

  @Override
  public Map<String, Object> displayData() {
    var data = super.displayData();
    if (name.equals("result")) {
      data.remove("name");
    }
    return data;
  }
}
