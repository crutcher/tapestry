package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@JsonTypeName("Label")
@TTagBase.SourceType(TTensor.class)
public final class TLabelTag extends TTagBase<TTensor> {
    @Nonnull
    @Getter
    private final String label;

    @JsonCreator
    public TLabelTag(
            @Nullable @JsonProperty(value = "id", required = true) UUID id,
            @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
            @Nonnull @JsonProperty(value = "label", required = true) String label
    ) {
        super(id, sourceId);
        this.label = label;
    }

    public TLabelTag(@Nonnull UUID sourceId, @Nonnull String label) {
        this(null, sourceId, label);
    }

    public TLabelTag(@Nonnull TLabelTag source) {
        this(source.id, source.sourceId, source.label);
    }

    @Override
    public TLabelTag copy() {
        return new TLabelTag(this);
    }
}
