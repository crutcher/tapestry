package loom.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import loom.zspace.ZPoint;

@JsonTypeName("Tensor")
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "box3d"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#d0d0ff")
    })
public class TTensor extends TNodeBase {
  @Nonnull @Getter public final ZPoint shape;

  @Nonnull @Getter public final String dtype;

  public TTensor(@Nullable UUID id, @Nonnull ZPoint shape, @Nonnull String dtype) {
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
