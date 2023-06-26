package loom.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("Observer")
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "Mcircle"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#B4F8C8"),
      @TNodeBase.DisplayOptions.Attribute(name = "margin", value = "0")
    })
public final class TObserver extends TSequencePoint {
  public TObserver(@Nullable UUID id) {
    super(id);
  }

  public TObserver() {
    this((UUID) null);
  }

  public TObserver(@Nonnull TObserver source) {
    this(source.id);
  }

  @Override
  public TObserver copy() {
    return new TObserver(this);
  }
}
