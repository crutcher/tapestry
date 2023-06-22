package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("HappensAfter")
@TTag.SourceType(TSequencedBase.class)
@TEdge.TargetType(TSequencePoint.class)
public final class THappensAfter extends TEdge {
  @JsonCreator
  THappensAfter(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
      @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId) {
    super(id, sourceId, targetId);
  }

  THappensAfter(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
    this(null, sourceId, targetId);
  }

  THappensAfter(@Nonnull THappensAfter source) {
    this(source.id, source.sourceId, source.targetId);
  }

  @Override
  public THappensAfter copy() {
    return new THappensAfter(this);
  }

  @JsonIgnore
  @Override
  public TSequencePoint getTarget() {
    return (TSequencePoint) super.getTarget();
  }
}
