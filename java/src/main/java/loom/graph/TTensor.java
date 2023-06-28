package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("Tensor")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "box3d"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "#d0d0ff")
    })
public class TTensor extends TNodeBase {
  @Nonnull @Getter public final ZPoint shape;

  @Nonnull @Getter public final String dtype;

  @JsonCreator
  public TTensor(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "shape", required = true) ZPoint shape,
      @Nonnull @JsonProperty(value = "dtype", required = true) String dtype) {
    super(id);
    this.shape = shape;
    this.dtype = dtype;
  }

  public TTensor(@Nonnull ZPoint shape, @Nonnull String dtype) {
    this(null, shape, dtype);
  }

  public TTensor(@Nonnull TTensor source) {
    this(source.id, source.shape, source.dtype);
  }

  @Override
  public TTensor copy() {
    return new TTensor(this);
  }

}
