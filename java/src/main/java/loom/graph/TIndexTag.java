package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import loom.zspace.ZRange;

@JsonTypeName("Index")
@TTagBase.SourceType(TBlockOperator.class)
public final class TIndexTag extends TTagBase<TTensor> {
  @Nonnull @Getter private final ZRange range;

  @JsonCreator
  public TIndexTag(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
      @Nonnull @JsonProperty(value = "range", required = true) ZRange range) {
    super(id, sourceId);
    this.range = range;
  }

  public TIndexTag(@Nonnull UUID sourceId, @Nonnull ZRange range) {
    this(null, sourceId, range);
  }

  public TIndexTag(@Nonnull TIndexTag source) {
    this(source.id, source.sourceId, source.range);
  }

  @Override
  public TIndexTag copy() {
    return new TIndexTag(this);
  }

  @Override
  public Map<String, Object> displayData() {
    var data = super.displayData();
    @SuppressWarnings("unchecked")
    var fields = (Map<String, Object>) data.remove("range");
    data.putAll(fields);
    return data;
  }
}
