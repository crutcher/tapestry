package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@JsonTypeName("Consumes")
@TTagBase.SourceType(TOperatorBase.class)
@TEdgeBase.TargetType(TTensor.class)
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {@TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#DDA6E0")})
public final class TConsumesEdge extends TEdgeBase<TOperatorBase, TTensor> {
  @Nonnull @Getter private final String name;

  @JsonCreator
  public TConsumesEdge(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
      @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId,
      @Nonnull @JsonProperty(value = "name", required = true) String name) {
    super(id, sourceId, targetId);
    this.name = name;
  }

  public TConsumesEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String name) {
    this(null, sourceId, targetId, name);
  }

  public TConsumesEdge(@Nonnull TConsumesEdge source) {
    this(source.id, source.sourceId, source.targetId, source.name);
  }

  @Override
  public TConsumesEdge copy() {
    return new TConsumesEdge(this);
  }

  @Override
  public Map<String, Object> displayData() {
    var data = super.displayData();
    if (name.equals("input")) {
      data.remove("name");
    }
    return data;
  }
}
