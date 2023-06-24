package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("HappensAfter")
@TTagBase.SourceType(TSequencedBase.class)
@TEdgeBase.TargetType(TSequencePoint.class)
public final class THappensAfterEdge extends TEdgeBase<TSequencedBase, TSequencePoint> {
  @JsonCreator
  public THappensAfterEdge(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
      @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId) {
    super(id, sourceId, targetId);
  }

  public THappensAfterEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
    this(null, sourceId, targetId);
  }

  public THappensAfterEdge(@Nonnull THappensAfterEdge source) {
    this(source.id, source.sourceId, source.targetId);
  }

  @Override
  public THappensAfterEdge copy() {
    return new THappensAfterEdge(this);
  }
}
