package loom.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("Tensor")
@TNodeBase.DisplayOptions.NodeShape("box3d")
@TNodeBase.DisplayOptions.BackgroundColor("#d0d0ff")
public class TTensor extends TNodeBase {
    @Nonnull
    @Getter
    public final ZPoint shape;

    @Nonnull
    @Getter
    public final String dtype;

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
