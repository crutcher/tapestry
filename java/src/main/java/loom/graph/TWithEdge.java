package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("With")
@TTagBase.SourceType(TOperatorBase.class)
@TEdgeBase.TargetType(TParameters.class)
public final class TWithEdge extends TEdgeBase<TOperatorBase, TParameters> {
  @JsonCreator
  public TWithEdge(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
      @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId) {
    super(id, sourceId, targetId);
  }

  public TWithEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
    this(null, sourceId, targetId);
  }

  public TWithEdge(@Nonnull TWithEdge source) {
    this(source.id, source.sourceId, source.targetId);
  }

  @Override
  public TWithEdge copy() {
    return new TWithEdge(this);
  }
}
