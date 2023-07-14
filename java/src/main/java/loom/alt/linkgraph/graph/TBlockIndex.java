package loom.alt.linkgraph.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import loom.zspace.ZRange;

@JsonTypeName("BlockIndex")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "tab"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "#E7DCB8"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "margin", value = "0.15")
    })
public class TBlockIndex extends TNodeBase {
  @Nonnull @Getter public final ZRange range;

  @JsonCreator
  public TBlockIndex(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "range") ZRange range) {
    super(id);
    this.range = range;
  }

  public TBlockIndex(@Nonnull ZRange range) {
    this(null, range);
  }

  public TBlockIndex(@Nonnull TBlockIndex source) {
    this(source.id, source.range);
  }

  @Override
  public TBlockIndex copy() {
    return new TBlockIndex(this);
  }

  @Override
  public Map<String, Object> displayData() {
    var data = super.displayData();
    @SuppressWarnings("unchecked")
    var rangeKeys = (Map<String, Object>) data.remove("range");
    if (rangeKeys != null) {
      for (var entry : rangeKeys.entrySet()) {
        data.put("@" + entry.getKey(), entry.getValue());
      }
    }
    return data;
  }

  public interface THasBlockIndexProperty extends TNodeInterface {
    default TBlockIndex bindIndex(TBlockIndex index) {
      assertGraph().addNode(new TWithIndexEdge(getId(), index.id));
      return index;
    }

    @CanIgnoreReturnValue
    default TBlockIndex bindIndex(ZRange range) {
      return bindIndex(assertGraph().addNode(new TBlockIndex(range)));
    }
  }

  @JsonTypeName("WithIndex")
  @TTagBase.SourceType(THasBlockIndexProperty.class)
  @TEdgeBase.TargetType(TBlockIndex.class)
  public static final class TWithIndexEdge extends TEdgeBase<THasBlockIndexProperty, TBlockIndex> {
    @JsonCreator
    public TWithIndexEdge(
        @Nullable @JsonProperty(value = "id", required = true) UUID id,
        @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
        @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId) {
      super(id, sourceId, targetId);
    }

    public TWithIndexEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
      this(null, sourceId, targetId);
    }

    public TWithIndexEdge(@Nonnull TWithIndexEdge source) {
      this(source.id, source.sourceId, source.targetId);
    }

    @Override
    public TWithIndexEdge copy() {
      return new TWithIndexEdge(this);
    }

    @Override
    public void validate() {
      super.validate();
      var conflictingEdges =
          assertGraph().queryEdges(TWithIndexEdge.class).withSourceId(getSourceId()).toList();
      if (conflictingEdges.size() > 1) {
        throw new IllegalStateException("Multiple parameters: " + conflictingEdges);
      }
    }
  }
}
